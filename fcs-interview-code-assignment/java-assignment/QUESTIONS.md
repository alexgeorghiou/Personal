# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
There are three strategies in here. Store uses the active record pattern, with the
entity being persisted straight from the resource. Product uses a Panache repository,
but the JPA entity is still what goes in and out over HTTP. Warehouse goes all the way
to a hexagonal setup: a domain model, a WarehouseStore port, a DbWarehouse entity and a
repository adapter that maps between the two.

I would not force all three into the same mould, but I would move Store to the same
shape as Product, and I would stop returning JPA entities from the resources.

The active record calls sitting in StoreResource are the ones I find most expensive to
live with. They pin every read and write to a static method on the entity, so the only
way to test the resource is to boot the application and a database, and the transaction
boundary ends up on the HTTP handler because there is nowhere else to put it. Task 2 is
a good illustration: the fix had to happen in the resource, because that is the only
place that knows about the transaction.

Exposing entities as the API contract is the other one. A rename of a column becomes a
breaking API change, and lazy associations tend to leak into the serializer.

The Warehouse layering costs a mapper and an extra type, and I would only pay that where
there are actual rules to protect, which is the case here. For Product, which is plain
CRUD today, a repository plus a small DTO is enough, and it can grow into ports and use
cases later if rules show up.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Generating from the spec keeps the contract and the code from drifting apart. The spec
is reviewable by people who are never going to read Java, it can be published to
consumers and used to generate their clients, and the compiler tells you when you break
it, because dropping an operation breaks the interface you implement. The price is an
indirection: the generator dictates the method signatures and the return types, so
things like answering 201 instead of 200, or shaping an error body, need a workaround
rather than a line of code. Debugging means reading generated sources, and you inherit
the generator's bugs and its upgrade cycle.

Hand written handlers are the opposite trade. Full control, nothing between you and
JAX-RS, easy to read. But the contract becomes an emergent property of the code. A spec
generated from annotations describes what was built rather than agreeing on what should
be built, and nothing stops a change in a handler from silently breaking a consumer.

My choice depends on who consumes the API. For anything crossing a team or a company
boundary I go spec first, as Warehouse does here, and accept the friction. For endpoints
that only serve our own UI or our own jobs I would code them directly and generate the
spec from the annotations, because the contract has a single consumer that ships with
it. What I would avoid is the third option, a hand written spec kept in sync with hand
written code by convention. That one always drifts.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I put most of the effort where the rules are, which here means the warehouse use cases.
They are plain objects behind ports, so an in memory WarehouseStore is enough to cover
every constraint and every rejection message in milliseconds, with no database and no
container. That is what I did, and it is also what makes those tests worth keeping: they
fail for a business reason, not because an unrelated column moved.

Above that I keep a thinner layer of endpoint tests that check the wiring rather than
the rules: status codes, payload mapping, the not found and bad request paths, and the
behaviour that only exists at runtime. The propagation to the legacy system is a good
example, since no unit test can prove it. The one that earns its place there is the test
asserting that a rolled back store never reaches the legacy system.

For the persistence side I lean on the real database through Dev Services rather than an
in memory substitute, and I use @TestTransaction so each test rolls back and the suite
stays independent of execution order.

On keeping it effective over time: coverage as a percentage is not a target I would
chase, since it rewards testing getters. What I watch instead is that every bug fix
arrives with a test that fails without it, that the suite stays fast enough to run
before every push, and that flaky or order dependent tests get fixed rather than
retried. Tests asserting on shared seed data are the usual source of that, which is why
the ones here create the data they need.
```
