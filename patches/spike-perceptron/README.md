# Spike Perceptron Patch

This folder contains a patch for Chipyard's native Spike submodule:

```text
toolchains/riscv-tools/riscv-isa-sim
```

The patch adds a lightweight C++ perceptron branch predictor model inside Spike and exposes:

```bash
--perceptron-stats
```

When enabled, Spike instruments conditional branches and prints:

```text
branches
mispredictions
miss_rate
```

## Apply

From the Chipyard repository root:

```bash
cd toolchains/riscv-tools/riscv-isa-sim
git apply ../../../patches/spike-perceptron/perceptron-spike.patch
```

## Build

From the Spike build directory:

```bash
cd toolchains/riscv-tools/riscv-isa-sim/build
env PATH=/scratch/eecs251b-aba/chipyard-jiaqihuang2025-glitch/.conda-env/bin:/scratch/eecs251b-aba/chipyard-jiaqihuang2025-glitch/.conda-env/riscv-tools/bin:$PATH make -j2 spike
```

## Run

Example:

```bash
env PATH=/scratch/eecs251b-aba/chipyard-jiaqihuang2025-glitch/.conda-env/bin:/scratch/eecs251b-aba/chipyard-jiaqihuang2025-glitch/.conda-env/riscv-tools/bin:$PATH \
toolchains/riscv-tools/riscv-isa-sim/build/spike \
  --perceptron-stats \
  toolchains/riscv-tools/riscv-pk/build/pk \
  /scratch/eecs251b-aba/riscv-isa-sim/data_dependent.riscv
```

Expected example output:

```text
bbl loader
done: 23923 4058831531
Perceptron stats hart=0: branches=1732024 mispredictions=413941 miss_rate=23.899%
```

This is an online Spike predictor model. It is not a cycle-accurate BOOM frontend model.
