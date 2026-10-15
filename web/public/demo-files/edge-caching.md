# Edge caching, three ways

Most pages on the shop change once a day, but we rendered them on every request.

## 1. Cache the HTML at the edge
A short `s-maxage` and `stale-while-revalidate` cut the median response from 410 ms to 38 ms.

## 2. Keep the cart out of the cache
The cart is the one personal part: it loads after the page, from its own endpoint.

## 3. Purge on publish
Publishing a product purges its page and the category, nothing else.

| page | before | after |
|---|---|---|
| home | 410 ms | 38 ms |
| product | 520 ms | 41 ms |
