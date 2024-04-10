PortalNetwork is a portal system that gives control back to the players to be
able to create portals that can dial each other. It supports different types of
portals and allows players to change the portal shape.  Vehicles are also
supported.

**Forked from [bundabrg/PortalNetwork](https://github.com/bundabrg/PortalNetwork) for Clod-MC**

## Features

* Players can create up to 256 networks of portals with up to 16 portals in
  each network.  Any portal can dial any other portal in the same network.

* Three different looks of portals.  Nether, End Gateway and Hidden (Invisible)
  Portals

* Portals can exist on different worlds and can dial each other across worlds.

* Vehicles like Minecarts, Horses, Pigs and boats will teleport as well and
  maintain their passengers.

* Blocks other than Obsidian can be placed in the portal area. This allows
  rails to be placed for example.

* Portals only need a base to be built. When they are dialled they will create
  their own frames if necessary and clean up after.

* Whatever enters one portal will maintain its relative orientation when
  existing another portal. This includes existing the back of the portal when
  entering the front

* Relative velocity is also maintained. This means flying through with an
  elytra works well.

* If a portal has one side blocked (for example it is against a wall) then the
  player existing it will be flipped 180 to exit out the other side. If both
  sides are blocked then you have a naughty player.

* If a portal has one side blocked (for example it is against a wall) then the
  player existing it will be flipped 180 to exit out the other side. If both
  sides are blocked then you have a naughty player.

* Configuration so that portals can be customized including what material they
  are when an item and what sounds they make.

* An API to allow plugins to add additional portal types.
