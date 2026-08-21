### Order Book

sorted map of the prices

separated into: bid and ask

Rules:
- a buy order must match the corresponding sell order
- a sell order must match the corresponding buy order
you need the orders to be sorted by price (best price first),
and within the same price, orders sorted by arrival time (first come, first served)

note: 
the best bid is the highest price someone is willing to pay
the best ask is the lowest price someone is willing to pay

TreeMap<Price, Deque<Order>>

# arrivaltime -> price
Map<Instant, Integer> orders = HashMap.of(
"time", 2000,
"time", 3000,
"time": 4000
)

new order
↓
determine side
↓
find compatible orders
↓
match orders
↓
execute trades
↓
update/remove orders
↓
publish market events


what happens when a new order arrives:
- Determine the opposite side of the book (a new buy order looks at asks, a new sell order looks at bids)
- Look at the best price level on that opposite side
- If the new order's price crosses the best opposite price 
(a buy at or above the best ask, or a sell at or below the best bid), a match can happen
- Match against the orders in that price level's queue, oldest first, filling as much as possible
- If the incoming order isn't fully filled and there's no more crossing price available, 
whatever remains gets added to the book on its own side, waiting for a future match