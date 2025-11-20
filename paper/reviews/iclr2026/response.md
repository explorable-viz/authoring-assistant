# Response to reviewers

We thank our three reviewers for their useful feedback and suggestions for improving the paper. We address
specific comments of each reviewer below, and then summarise our planned revisions to the paper.

## Reviewer AyvP

_Restriction to single papers._ Nothing about our design restricts it to single papers, although for our
evaluation we have only considered excerpts from single papers. A multi-paper setup is an interesting use case
and we will consider for a real-world tool.

_Checks for more complex claims cannot be generated with LLMs. Even for the simple cases, a human has to
verify the generated links._ Keeping a human in the loop for verification purposes seems important from a
trust point of view. For the simple cases, where it may seem unfortunate that author verification is still
needed, a small amount of author effort nevertheless translates into a significant _reader_ benefit (since
there are many more readers than authors). In future work we anticipate automated testing as a way of reducing
the author burden by automatically exercising the generated code.

For the more complex cases, where current LLMs are unable to generate a full solution in one step, we envisage
a more interactive workflow, similar to working with Copilot. For the present paper we are focused on
exploring the limits of current LLM capabilities and validating a simpler architecture with less interaction.

## Reviewer Bezq

_Can authors report statistics about the manual validation process?_

Unfortunately a user study is out-of-scope for this work; see [...]

_Re. the subset of SciGen used, could you provide details on the selection logic, sampling criteria, and exact
number of examples used in evaluation?_

[Number of example papers + replacement problems within each of those]

The paper states that experiments were conducted on “a subsample of the SciGen dataset” (line 367), but it remains unclear how this subset was chosen. Could the authors provide more details on the selection logic, sampling criteria, and the exact number of examples used in evaluation?

It seems this framework rely on predefined helper routines such as trendWord or growShrink, which encode semantic logic that the model merely invokes rather than learns. Moreover, the large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning. Could the authors conduct an ablation study removing or varying these helper components to determine how much of the system’s success derives from the LLM’s own reasoning versus predefined components?

For the submitted version we relied on a small subset of SciGen, with only XX example replacement tasks.

## Planned revisions to paper

