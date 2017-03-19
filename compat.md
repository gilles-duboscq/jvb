# TODO

* Test line shift mode (e.g., Insmouse No Yakata)
* Fix VIP crashes with object window
* Fix Display Ready bit
* GamePad manual mode
* VSU
* Fix exceptions
* BitString
* General CPU completeness pass

# ROM compatibility

| Title | Notes
| --- | ---
| 3D Tetris (U) | Bitstring (VSU)
| affine_demo_1 | ✓ (read from uninitialized?)
| affine_demo_2 | ✓
| Blox V1.0 by KR155E (PD) | Too fast, some missing graphics?
| Blox V1.1 by KR155E (PD) | ✓ (VSU)
| Bound High! (JU) | Various VIP glitches/issues: object mode crash, horizontal stripes (VSU)
| Croach, The (PD) | Nothing happens (waiting for drawing status to change while drawing is disabled?)
| Etch-A-Sketch by Pat Daderko (PD) | ✓
| Framebuffer Drawing Demo by Pat Daderko (PD) | Strange band at the top
| Galactic Pinball (JU) | VIP crash (object mode) (VSU)
| GLOW Demo by KR155E (PD) [a1] | ✓
| GLOW Demo by KR155E (PD) | ✓
| Golf (U) | Bitstring
| Hello, World! Demo V1.0 by Amos Bieler (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Hello, World! Demo V1.1 by Amos Bieler (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Insmouse No Yakata (J) / Insane Mouse Mansion (J) | VIP crash
| Jack Bros (J) | Glitchy/non functional instructions screen (VSU)
| Jack Bros (U) | Glitchy/non functional instructions screen (VSU)
| Mario Clash (JU) | Does not register key on instructions screen (manual gamepad reading?)
| Mario Flying Demo by Frostgiant (PD) | ✓ (Seems to use uninitialized memory as a "black" char)
| Mario's Tennis (JU) [a1] | Glitchy graphics (VIP memory corruption?)
| Mario's Tennis (JU) | illegal instruction?
| Matrix, The by Cooler (PD) | Glitchy, way too fast
| Nester's Funky Bowling (U) [a1] | bitstring
| Nester's Funky Bowling (U) | bitstring
| OBJ Pointer Demo by Dan Bergman (PD) | ✓
| Panic Bomber (J) | VIP object mode crash (VSU)
| Panic Bomber (U) | VIP object mode crash (VSU)
| pong | nothing happens (waiting for drawing status to change while drawing is disabled?)
| Reality Boy Demo 1 (PD) | ✓
| Reality Boy Demo 2 (PD) | Link
| Red Alarm (J) | Hangs after some intro screens (waiting for an interrupt?) (VSU)
| Red Alarm (U) | Hangs after some intro screens (waiting for an interrupt?) (VSU)
| Scaling Demo by Parasyte (PD) | ✓ (Does not initialize window param memory properly, use -Djvb.ram.init=0)
| SD Gundam Dimension War (J) | VIP crash (objects)
| Simon by Pat Daderko (PD) | ✓
| Space Invaders: Virtual Collection (J) | Glitchy graphcis, seems to wait for interrupt
| Space Squash (J) | VIP object mode crash (VSU)
| Super Fighter Demo by KR155E (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Teleroboxer (JU) [T+Ger.4b_KR155E] | Stuck on instructions (manual gamepad input?) (VSU)
| Teleroboxer (JU) | Stuck on instructions (manual gamepad input?) (VSU)
| T&E Virtual Golf (J) | Bitstring
| Tic Tac Toe by Pat Daderko (PD) | Link
| Tron VB by Pat Daderko (PD) | Link
| VB Rocks! Demo by KR155E (PD) | Displays dead data from fb (waiting for display *not* ready?)
| vb_test_2 | Changing OOB char affects other fields
| VeeBee Cursor Demo by David Williamson (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Vertical Force (J) | Nothing happens (waiting for display *not* ready?) (VSU)
| Vertical Force (U) | Nothing happens (waiting for display *not* ready?) (VSU)
| Virtual Bowling (J) | VIP crash (oob read in bg/param ram)
| Virtual Boy Wario Land (JU) | Nothing happens after instruction & focus screen
| Virtual-E Cursor Demo (PD) [a1] | Nothing happens (waiting for display *not* ready?) 
| Virtual-E Cursor Demo (PD) | Bad rom?
| Virtual Fishing (J) | VIP object mode crash & crash/reset after game's splash screen.
| Virtual Lab (J) | Falling pieces are not displayed
| Virtual League Baseball (U) [a1] | GamePad used in manual mode in menu does not work
| Virtual League Baseball (U) | illegal op?
| Virtual Pong (PD) | Nothing happens (waiting for drawing status to change while drawing is disabled?)
| Virtual Pro Yakyuu '95 (J) | Crashes when playing a ball
| V Tetris (J) | VIP crash
| Waterworld (U) | VIP crash