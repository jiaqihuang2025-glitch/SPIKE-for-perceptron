#include "mmio.h"

#define SRAM_BIST_DATA0 0x4000
#define SRAM_BIST_DATA1 0x4004
#define SRAM_BIST_DONE 0x4008
#define SRAM_BIST_FAIL 0x400C
#define SRAM_BIST_EX 0x4010

int main(void)
{
  reg_write32(SRAM_BIST_DATA0, 0);
  reg_write32(SRAM_BIST_DATA1, 0xffffffff);

  reg_write8(SRAM_BIST_EX, 1);

  // wait for peripheral to complete
  while ((reg_read8(SRAM_BIST_DONE) & 0x1) == 0) ;

  if ((reg_read8(SRAM_BIST_FAIL ) & 0x1) == 0) {
      printf("SRAM BIST succeeded!\n");
  } else {
      printf("SRAM BIST failed!\n");
  }

  reg_write32(SRAM_BIST_DATA0, 0xcafef00d);
  reg_write32(SRAM_BIST_DATA1, 0xdeadbeef);

  reg_write8(SRAM_BIST_EX, 1);

  // wait for peripheral to complete
  while ((reg_read8(SRAM_BIST_DONE) & 0x1) == 0) ;

  if ((reg_read8(SRAM_BIST_FAIL ) & 0x1) == 0) {
      printf("SRAM BIST succeeded!\n");
  } else {
      printf("SRAM BIST failed!\n");
  }

  reg_write32(SRAM_BIST_DATA0, 0xdeadbeef);
  reg_write32(SRAM_BIST_DATA1, 0xf00dd00d);

  reg_write8(SRAM_BIST_EX, 1);

  // wait for peripheral to complete
  while ((reg_read8(SRAM_BIST_DONE) & 0x1) == 0) ;

  if ((reg_read8(SRAM_BIST_FAIL ) & 0x1) == 0) {
      printf("SRAM BIST succeeded!\n");
  } else {
      printf("SRAM BIST failed!\n");
  }

  return 0;
}
