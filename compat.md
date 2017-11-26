# TODO

* Fix VIP timings
* Fix exceptions
* General CPU completeness pass
* Fix Display Ready bit

# ROM compatibility

| Title | Notes
| --- | ---
| 3D Tetris (U) | Hangs on instructions screen
| affine_demo_1 | ✓
| affine_demo_2 | ✓
| Blox V1.0 by KR155E (PD) | Too fast, some missing graphics?
| Blox V1.1 by KR155E (PD) | ✓ (VSU)
| Bound High! (JU) | Glitchy game screens. Execution goes wrong for demo mode and when dying in game
| Croach, The (PD) | Nothing happens (waiting for drawing status to change while drawing is disabled?)
| Etch-A-Sketch by Pat Daderko (PD) | ✓
| Framebuffer Drawing Demo by Pat Daderko (PD) | Strange band at the top
| Galactic Pinball (JU) | Sound goes wrong while playing
| GLOW Demo by KR155E (PD) [a1] | ✓
| GLOW Demo by KR155E (PD) | ✓
| Golf (U) | Bitstring
| Hello, World! Demo V1.0 by Amos Bieler (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Hello, World! Demo V1.1 by Amos Bieler (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Insmouse No Yakata (J) / Insane Mouse Mansion (J) | Password screen does not display correctly, bubbles are not working properly
| Jack Bros (J) | Glitchy/non functional instructions screen (VSU)
| Jack Bros (U) | Glitchy/non functional instructions screen (VSU)
| Mario Clash (JU) | No character in game?
| Mario Flying Demo by Frostgiant (PD) | ✓ (Seems to use uninitialized memory as a "black" char)
| Mario's Tennis (JU) [a1] | Very glitchy graphics (VIP memory corruption?)
| Mario's Tennis (JU) | illegal instruction?
| Matrix, The by Cooler (PD) | Glitchy, way too fast
| Nester's Funky Bowling (U) [a1] | Crashes when throwing the ball
| Nester's Funky Bowling (U) | bad rom?
| OBJ Pointer Demo by Dan Bergman (PD) | ✓
| Panic Bomber (J) | Sound does not work: wave index is never set. Writes in read-only memory.
| Panic Bomber (U) | Sound does not work: wave index is never set. Writes in read-only memory.
| pong | nothing happens (waiting for drawing status to change while drawing is disabled?)
| Reality Boy Demo 1 (PD) | ✓
| Reality Boy Demo 2 (PD) | Link
| Red Alarm (J) | Hangs
| Red Alarm (U) | Hangs
| Scaling Demo by Parasyte (PD) | ✓ (Does not initialize window param memory properly, use `-Djvb.ram.init=0`)
| SD Gundam Dimension War (J) | Glitchy graphics/hangs
| Simon by Pat Daderko (PD) | ✓
| Space Invaders: Virtual Collection (J) | Glitchy graphics, seems to wait for interrupt
| Space Squash (J) | Hangs (VSU)
| Super Fighter Demo by KR155E (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Teleroboxer (JU) [T+Ger.4b_KR155E] | ✓
| Teleroboxer (JU) | ✓
| T&E Virtual Golf (J) | Unimplemented bitstring cases
| Tic Tac Toe by Pat Daderko (PD) | Link
| Tron VB by Pat Daderko (PD) | Link
| VB Rocks! Demo by KR155E (PD) | Displays dead data from fb (waiting for display *not* ready?)
| vb_test_2 | ✓
| VeeBee Cursor Demo by David Williamson (PD) | Displays dead data from fb (waiting for display *not* ready?)
| Vertical Force (J) | Sound is wrong (VSU state doesn't seem to be set properly).
| Vertical Force (U) | Sound is wrong (VSU state doesn't seem to be set properly).
| Virtual Bowling (J) | VIP crash (oob read in bg/param ram)
| Virtual Boy Wario Land (JU) | Nothing happens after instruction & focus screen (timer issue?)
| Virtual-E Cursor Demo (PD) [a1] | Nothing happens (waiting for display *not* ready?) 
| Virtual-E Cursor Demo (PD) | Bad rom?
| Virtual Fishing (J) | ✓
| Virtual Lab (J) | Falling pieces are not displayed
| Virtual League Baseball (U) [a1] | Controls are a bit strange, keeps on auto-pausing
| Virtual League Baseball (U) | illegal op?
| Virtual Pong (PD) | Nothing happens (waiting for drawing status to change while drawing is disabled?)
| Virtual Pro Yakyuu '95 (J) | Controls don't always seem to work in menus
| V Tetris (J) | ✓ (VSU)
| Waterworld (U) | Glitchy