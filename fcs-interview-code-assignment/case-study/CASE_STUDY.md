# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The hard part is not capturing the costs, it is attributing them. Almost nothing in this
domain belongs to a single warehouse, store or product on its own. A shift covers many
products, a truck serves several stores on one route, and rent and systems are fixed per
site regardless of what moves through it. So every number below the level of "what we
actually paid" depends on an allocation driver, and the choice of driver is a business
agreement rather than a technical fact. Per order, per unit, per cubic metre and per
labour hour will each produce a defensible and different answer for the same month.

Two things I would watch closely. First, double counting: this model already allows a
product to be fulfilled by two warehouses for the same store, so any per product cost
has to be split rather than counted twice. Second, time. Payroll lands monthly, fuel
daily, and supplier invoices arrive thirty to sixty days late, which means the cost of a
warehouse in March keeps changing well into May. That argues for append only, effective
dated cost records and an explicit restatement policy, so a report run twice gives the
same answer twice.

The closest experience I can draw on is reconciliation work on a payments platform,
where the lesson was that any figure shown to finance has to be traceable back to the
source event that produced it. A total with no audit trail creates more work than it
saves, because the first question is always where the number came from.

Questions I would want answered before scoping this:

- Who consumes the numbers, finance at close or operations on a dashboard? That decides
  the trade off between accuracy and latency, and the two audiences rarely want the same
  thing.
- What is the smallest unit finance actually needs, warehouse, store, product or order?
  Going finer than that is expensive and mostly generates argument.
- Do the allocation drivers already exist, or are we inventing them? If we are inventing
  them, finance has to own that decision, not engineering.
- Is there an ERP that is already the book of record? If so we are a feeder system and
  should not be producing competing totals.
- What is the accounting calendar, and can closed periods be restated or do late costs
  land in the current one?
- Multiple currencies or legal entities?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

The obvious levers in this domain are capacity utilisation, fulfilment assignment,
inventory holding, labour scheduling and shipment consolidation. Two of them are already
visible in the model. Warehouses carry a capacity and a stock, and locations cap both the
number of warehouses and the total capacity, so unused capacity is space we are paying
for and colocation decisions have a direct cost. The fulfilment feature is the other one:
deciding which warehouse serves which product for which store is largely a transport cost
decision, and the limit of two warehouses per product per store is a real trade off
between cost and resilience rather than an arbitrary rule.

Nothing here can be prioritised before scenario 1 exists. Without a baseline, every
proposal is an opinion. Once there is one, I would rank candidates by the size of the
cost pool multiplied by the share we can realistically influence, divided by the effort
to get there, and be honest that a large pool we do not control, such as a signed lease,
is not an opportunity.

The failure I would guard against is moving cost rather than removing it. Cutting
transport by consolidating shipments is easy to measure and can quietly show up as
stockouts, overtime or missed delivery windows. So every initiative should be stated as a
hypothesis with a target and a guardrail metric, for example reduce transport cost per
order by eight percent without on time delivery falling below its current level.

On implementation I would pilot on a subset of stores and warehouses, keep a comparable
group untouched, measure both metrics, and only then roll out. An optimisation engine
delivered big bang tends to be switched off the first time it produces a recommendation
that someone on the ground knows is wrong.

Questions I would ask:

- How much of the cost base is genuinely controllable rather than fixed by leases,
  contracts or labour agreements?
- What service level commitments constrain us, and are they contractual or internal?
- Can fulfilment assignments actually be changed operationally, or are they fixed by
  agreements with the stores?
- What is the decision cadence, daily routing or annual network design? They need very
  different systems.
- Who makes the final call? If a human overrides every recommendation, we are building a
  report, and it should be scoped and priced as one.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

The benefit is that the two sides hold different halves of the same picture. The finance
system knows what was paid and against which account, and this tool knows the operational
context, which warehouse, which store, which product, that the finance system has no way
to represent. Joining them removes a lot of manual re-keying and shortens the close, but
the value is really that cost figures become explainable rather than just correct.

The first question is direction and ownership, and it is usually settled before any
technical discussion. If anything posts to the general ledger, the ERP is the book of
record and we are a feeder. We push cost events out and pull master data in, chart of
accounts, cost centres, vendors. The real work is master data alignment: our business
unit code has to map to their cost centre, and that mapping has to survive a warehouse
replacement, which is exactly the case where a naive mapping breaks.

I would also push back on real time as a blanket requirement. The ledger closes in
periods, not continuously, and real time posting to the GL is rarely wanted or allowed.
Operational dashboards are where latency matters. So I would split it, event driven for
the operational view and a reconciled periodic feed for accounting, and be explicit about
which one a given number came from.

Technically the things that decide whether this works are idempotency and
reconciliation. Every message needs a stable business key so the receiver can dedupe,
because financial feeds get replayed and a double post is much worse than a late one.
Publishing should go through an outbox so nothing is emitted for a transaction that
rolled back, which is the same problem as the legacy store synchronisation in the code
assignment. And there should be a scheduled comparison of totals per period with an alert
on divergence, since the failure mode of these integrations is silent drift rather than a
loud error.

Questions I would ask:

- Which system is the book of record for cost, and does that answer change by cost type?
- What integration surface does the finance system actually offer, an API, flat files, a
  staging schema?
- What is the close calendar and the cutoff, and what happens to costs that arrive after
  it?
- Who reconciles today, and how would they check our feed?
- Multi currency, and if so whose FX rates and as of when?
- What are the audit and retention requirements? They usually dictate the storage model
  more than anything else.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Tracking cost without a forecast only tells you what already happened. The budget is what
turns it into a control, because it gives a number to compare against and a threshold for
acting. In this domain it also drives capacity decisions directly, since the question of
whether a location needs another warehouse is a forecast question before it is anything
else.

The design point I care most about is to forecast volume first and cost second. Orders
and units per store per period are things the business can reason about and argue over
productively, and cost then follows from rates applied to that volume. Forecasting cost
directly hides the assumptions and makes every variance discussion unresolvable. That
also forces a clean split between fixed and variable cost, because only the variable part
should move with volume, and in fulfilment the fixed share is large.

Budgets are versioned artifacts, not current values. The original budget, each reforecast
and the actuals all have to coexist, because variance analysis is a comparison between
versions. Overwriting is the most common way these systems become useless, and it is the
same append only argument as in the cost model.

On granularity I would deliberately stay coarser than the cost tracking, at cost centre
and period rather than per product. Budgeting at a finer grain than anyone can act on
produces false precision and a steady stream of variances nobody is going to investigate.

Seasonality, promotions, store openings and warehouse replacements should be explicit
inputs rather than something the model is expected to infer from history, since they are
known in advance and are exactly the periods where a purely historical fit will be wrong.

Finally the forecast itself needs to be measured. Tracking error per cost pool over time
is what tells you which parts of the model to trust and where to spend effort improving
it.

Questions I would ask:

- What is the planning cycle, annual budget with quarterly reforecast, or something more
  continuous?
- Who owns the numbers, finance, operations or both, and how are disagreements settled?
- How much clean history is there? Rates fitted on two years of messy data are a
  different proposition from ten years of good data.
- Which costs are contractually committed regardless of volume?
- Do we need bottom up submissions from sites as well as top down targets? That doubles
  the workflow, so it should be a deliberate choice.
- What variance threshold triggers action, and who acts?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

This is the scenario that maps most directly onto the data model. Archiving the old unit
and creating a new row under the same business unit code is exactly the shape cost
reporting needs, because it gives two different views of the same thing. The business
unit code is the continuous series, which is what you want for a trend across the area.
The individual warehouse row is the discrete facility, which is what you want when
comparing the old site against the new one. Costs have to attach to the row, not to the
code, or the two facilities blend into one and the comparison becomes impossible. The
created and archived timestamps on each row are what make both views available.

Preserving the history matters for four separate reasons. It is the baseline for judging
whether the new site is actually cheaper, which is usually the entire business case. It
keeps period and year on year reporting stable, since rewriting history changes numbers
that have already been reported. Anything already posted to the ledger cannot be
reattributed after the fact. And the replacement carries one off costs, fit out, moving
stock, running both sites in parallel, decommissioning, which must be kept separate from
the new site's ongoing run rate. Smearing them in makes a perfectly healthy facility look
permanently over budget in its first year.

On the budget itself, I would give the new warehouse its own baseline rather than
inheriting the old one. The capacity and cost structure are different, and in this model
the replacement can be created with a different capacity, so the old run rate is the
reference point for the business case rather than the target.

The part I would want defined explicitly is the transition window. There is usually a
period where both sites are live or stock is moving between them, and cost attribution
during that window needs an agreed effective date rather than being decided later by
whoever runs the report.

Questions I would ask:

- How are the one off transition costs treated, capitalised or expensed, and against
  which unit?
- Is there an overlap period, and who owns the cost incurred during it?
- Does anyone track the replacement business case against actuals afterwards, and over
  what horizon?
- For comparability, do we present the two facilities as one continuous series or side by
  side? Both are defensible and the reporting has to pick one as the default.
- Does the same pattern need to work for a site that closes without a replacement, or for
  one that is split into two? If so the model should be designed for that now rather than
  retrofitted.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
