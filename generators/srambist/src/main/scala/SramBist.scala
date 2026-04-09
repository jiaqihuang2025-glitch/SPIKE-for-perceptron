package srambist

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._

/** Access patterns that can be exercised by the BIST.
  *
  *   W0 - Write `data0` to each address sequentially, starting from address 0.
  *   W1 - Write `data1` to each address sequentially, starting from address 0.
  *   R0 - Reads each address sequentially and validates that it equals `data0`, 
  *        starting from address 0.
  *   R1 - Reads each address sequentially and validates that it equals `data1`, 
  *        starting from address 0.
  */
object Pattern extends Enumeration {
  type Type = Value
  val W0, W1, R0, R1 = Value
}

/** Runs a set of patterns against an SRAM to verify its behavior.
  *
  * Once the BIST is complete, `done` should be set high and remain high. If the BIST
  * failed, `fail` should be asserted on the same cycle and remain high.
  */
class SramBist(numWords: Int, dataWidth: Int, patterns: Seq[Pattern.Type])
    extends Module {
  val io = IO(new Bundle {
    // SRAM interface
    val we = Output(Bool())
    val addr = Output(UInt(log2Ceil(numWords).W))
    val din = Output(UInt(dataWidth.W))
    val dout = Input(UInt(dataWidth.W))

    // BIST interface
    val data0 = Input(UInt(dataWidth.W))
    val data1 = Input(UInt(dataWidth.W))
    val done = Output(Bool())
    val fail = Output(Bool())
  })

    val ctr = RegInit(0.U(log2Ceil(math.max(patterns.length, 1)).W))		// Control for which pattern
    val addrW = log2Ceil(numWords)		// Address Width
    val addrCnt = RegInit(0.U(addrW.W))		// Initial value to control within a pattern
    val doneReg = RegInit(false.B)		// Reg for done
    val failReg = RegInit(false.B)		// Reg for fail

	io.done := doneReg
	io.fail := failReg
	
	io.we   := false.B
	io.addr := addrCnt
	io.din  := 0.U

	// Reg for pattern
	val lastAddr = addrCnt === (numWords - 1).U
	val lastPat  = ctr === (patterns.length - 1).U

	// Reg for data
	val checkNext = RegInit(false.B)   // false for read, true for check
	val expReg    = RegInit(0.U(dataWidth.W)) // expected data

	patterns.zipWithIndex.foreach { // A Scala `foreach` loop to iterate over the Scala sequence `patterns`.
  		case (pattern, idx) => {
    		when(ctr === idx.U) { // Chisel `when` statement to match against the hardware register `ctr`.
      			pattern match { // Scala `match` statement to match against a specific Scala `Pattern` enumeration.
        			case Pattern.W0 => {
          				// Pattern W0 behavior
						io.we	:= true.B
						io.addr := addrCnt
						io.din	:= io.data0
						
						when(lastAddr){
							addrCnt := 0.U
							
							when(lastPat){
								doneReg := true.B
								failReg := false.B
							}.otherwise {
								ctr := ctr + 1.U
								checkNext := false.B
							}
						}.otherwise {
							addrCnt := addrCnt + 1.U
						}
        			}
        			case Pattern.W1 => {
          				// Pattern W1 behavior
						io.we	:= true.B
						io.addr := addrCnt
						io.din	:= io.data1
						
						when(lastAddr){
							addrCnt := 0.U
							
							when(lastPat){
								doneReg := true.B
								failReg := false.B
							}.otherwise {
								ctr := ctr + 1.U
								checkNext := false.B
							}
						}.otherwise {
							addrCnt := addrCnt + 1.U
						}
        			}
        			case Pattern.R0 => {
          				// Pattern R0 behavior

						when(!checkNext) {
							io.we	:= false.B						
							io.addr	:= addrCnt
							expReg	:= io.data0 
							checkNext	:= true.B
						}.otherwise {

							when(io.dout =/= expReg){
								doneReg := true.B
								failReg := true.B

							}.otherwise {

								when(lastAddr){
									addrCnt := 0.U
									checkNext := false.B
							
									when(lastPat){
										doneReg := true.B
										failReg := false.B
									}.otherwise {

										ctr := ctr + 1.U
									}
								}.otherwise {
									addrCnt := addrCnt + 1.U
									io.addr := addrCnt + 1.U
								}
							}
						}
        			}
        			case Pattern.R1 => {
          				// Pattern R1 behavior
						when(!checkNext) {
							io.we	:= false.B						
							io.addr	:= addrCnt
							expReg	:= io.data1
							checkNext	:= true.B

						}.otherwise {

							when(io.dout =/= expReg){
								doneReg := true.B
								failReg := true.B
							}.otherwise {

								when(lastAddr){
									addrCnt := 0.U
									checkNext	:= false.B
							
									when(lastPat){
										doneReg := true.B
										failReg := false.B
									}.otherwise {
										ctr := ctr + 1.U
									}
								}.otherwise {
									addrCnt := addrCnt + 1.U
									io.addr := addrCnt + 1.U
								}
							}
						}
        			}
      			}
    		}
  		}
	};

}

class SramBlackBox(numWords: Int, dataWidth: Int, muxRatio: Int, maskGranularity: Int)
    extends BlackBox
    with HasBlackBoxResource {
  val wmaskWidth = dataWidth / maskGranularity
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val we = Input(Bool())
    val wmask = Input(UInt(wmaskWidth.W))
    val addr = Input(UInt(log2Ceil(numWords).W))
    val din = Input(UInt(dataWidth.W))
    val dout = Output(UInt(dataWidth.W))
  })

  override val desiredName =
    s"sram22_${numWords}x${dataWidth}m${muxRatio}w${maskGranularity}_macro"

  // TODO: Point to the verilog source file that is we just defined at
  // `generators/srambist/src/main/resources/vsrc/sram22_64x32m4w32_macro.v`.
  //
  // Instead of hardcoding the SRAM name, use `desiredName`.
  	addResource(s"/vsrc/${desiredName}.v")
}

// The SRAM BIST doesn't expose any ports outside of its MMIO registers.
trait SramBistTopIO extends Bundle {}

trait SramBistTop extends HasRegMap {
  val io: SramBistTopIO
  val clock: Clock
  val reset: Reset

  val data0 = RegInit(0.U(32.W))
  val data1 = RegInit(0.U(32.W))
  val ex = Wire(new DecoupledIO(Bool()))
  val bistReset = Wire(Bool());

  // Allow restart of BIST at any point.
  ex.ready := true.B
  // Hold reset for at least one full cycle after EX register written.
  val pastValid = RegInit(false.B)
  pastValid := ex.valid
  bistReset := false.B
  when (ex.valid || pastValid) {
    bistReset := true.B
  }

  val bist = withReset(bistReset) {
    Module(new SramBist(64, 32, Seq(Pattern.W0, Pattern.R0, Pattern.W1, Pattern.R1)))
  }
  val sram = Module(new SramBlackBox(64, 32, 4, 32))

  sram.io.clk := clock

  // TODO: Connect up the remainder of `bist` and `sram` ports.
  sram.io.we 	:= bist.io.we
  sram.io.addr 	:= bist.io.addr
  sram.io.din 	:= bist.io.din
  bist.io.dout 	:= sram.io.dout
  sram.io.wmask	:= 1.U

  bist.io.data0 := data0
  bist.io.data1 := data1

  regmap(
    0x00 -> Seq(
      RegField.w(32, data0)
    ),
    0x04 -> Seq(
      RegField.w(32, data1)
    ),
    0x08 -> Seq(
      RegField.r(1, bist.io.done)
    ),
    0x0C-> Seq(
      RegField.r(1, bist.io.fail)
    ),
    0x10 -> Seq(
      RegField.w(1, ex)
    ),
  )
}

class SramBistTL(params: SramBistParams, beatBytes: Int)(implicit p: Parameters)
  extends TLRegisterRouter(
    params.address, "srambist", Seq("eecs251b,srambist"),
    beatBytes = beatBytes)(
      new TLRegBundle(params, _) with SramBistTopIO)(
      new TLRegModule(params, _, _) with SramBistTop)

case class SramBistParams(
  address: BigInt = 0x4000,
)

case object SramBistKey extends Field[Option[SramBistParams]](None)

trait CanHavePeripherySramBist { this: BaseSubsystem =>
  private val portName = "srambist"

  // If `SramBistKey` is declared, initialize and connect the BIST peripheral.
  val srambist = p(SramBistKey) match {
    case Some(params) => {
      val srambist = LazyModule(new SramBistTL(params, pbus.beatBytes)(p))
      pbus.coupleTo(portName) { srambist.node := TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _ }
      Some(srambist)
    }
    case None => None
  }
}

trait CanHavePeripherySramBistImp extends LazyModuleImp {
  val outer: CanHavePeripherySramBist
}


class WithSramBist(params: SramBistParams) extends Config((site, here, up) => {
  // Store the parameters in the configuration under `SramBistKey`.
  case SramBistKey => Some(params)
})


