#include <stdio.h>
#include <stdint.h>
#include "XOR_parity.h"

#define read_csr_safe(reg) ({ \
  register unsigned long __tmp; \
  asm volatile ("csrr %0, " #reg : "=r"(__tmp)); \
  __tmp; })

#define write_csr_safe(reg, val) ({ \
  unsigned long __v = (unsigned long)(val); \
  asm volatile ("csrw " #reg ", %0" :: "rK"(__v)); })

int main(void) {
  // Enable counters
  write_csr_safe(mcounteren, -1UL);
  write_csr_safe(scounteren, -1UL);
  write_csr_safe(mcountinhibit, 0);

  // Reset counters
  write_csr_safe(mhpmcounter3, 0);
  write_csr_safe(mhpmcounter4, 0);
  write_csr_safe(mhpmcounter5, 0);

  // Program events
  // Event selector encoding: (mask << 8) | set_id
  // set_id=1 -> BOOM branch-related event set in exu/core.scala
  write_csr_safe(mhpmevent3, 0x201);   // branch misprediction (mask bit1)
  write_csr_safe(mhpmevent4, 0x1001);  // branch resolved (mask bit4)
  write_csr_safe(mhpmevent5, 0x401);   // control-flow target misprediction/JALR (mask bit2)

  // Read initial values
  uint64_t p0   = read_csr_safe(mhpmcounter3);
  uint64_t p2   = read_csr_safe(mhpmcounter4);
  uint64_t p4   = read_csr_safe(mhpmcounter5);
  uint64_t instret_i = read_csr_safe(minstret);

  // -------- run benchmark here --------
  run_xor_data_phase_test(1000ULL, 256);
  // ------------------------------------

  // Read final values
  uint64_t p1   = read_csr_safe(mhpmcounter3);
  uint64_t p3   = read_csr_safe(mhpmcounter4);
  uint64_t p5   = read_csr_safe(mhpmcounter5);
  uint64_t instret_f = read_csr_safe(minstret);

  uint64_t mispredict_BR   = p1 - p0;
  uint64_t branch_resolved = p3 - p2;
  uint64_t mispredict_JALR = p5 - p4;
  uint64_t Ins_commit      = instret_f - instret_i;

  printf("Branch Mispredict before = %lu after = %lu delta = %lu\n",
         (unsigned long)p0, (unsigned long)p1, (unsigned long)mispredict_BR);
  printf("Branch Resolved before = %lu after = %lu delta = %lu\n",
         (unsigned long)p2, (unsigned long)p3, (unsigned long)branch_resolved);
  printf("JALR Mispredict before = %lu after = %lu delta = %lu\n",
         (unsigned long)p4, (unsigned long)p5, (unsigned long)mispredict_JALR);

  printf("InstRet before = %lu after = %lu delta = %lu\n",
         (unsigned long)instret_i, (unsigned long)instret_f, (unsigned long)Ins_commit);

  if (Ins_commit != 0) {
    // Multiply by 1,000,000 to get 3 decimal places of precision for MPKI
    uint64_t mpki_br_scaled   = (mispredict_BR * 1000000) / Ins_commit;
    uint64_t mpki_jalr_scaled = (mispredict_JALR * 1000000) / Ins_commit;

    printf("Mispredicts: %lu.%03lu\n",
           (unsigned long)(mpki_br_scaled / 1000),
           (unsigned long)(mpki_br_scaled % 1000));

    printf("Mispredicts (BR): %lu.%03lu\n",
           (unsigned long)(mpki_br_scaled / 1000),
           (unsigned long)(mpki_br_scaled % 1000));

    printf("Mispredicts (JALR): %lu.%03lu\n",
           (unsigned long)(mpki_jalr_scaled / 1000),
           (unsigned long)(mpki_jalr_scaled % 1000));

    if (branch_resolved != 0) {
      uint64_t mispred_pct_scaled = (mispredict_BR * 10000) / branch_resolved;
      printf("Mispredict Rate: %lu.%02lu%%\n",
             (unsigned long)(mispred_pct_scaled / 100),
             (unsigned long)(mispred_pct_scaled % 100));
    } else {
      printf("Mispredict Rate: branch_resolved is zero\n");
    }
  } else {
    printf("Instructions committed is zero\n");
  }

  printf("Instructions Committed: %lu\n", (unsigned long)Ins_commit);

  return 0;
}
