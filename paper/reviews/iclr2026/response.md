# Response to reviewers

We thank our three reviewers for their useful feedback and suggestions for improving the paper. We address
specific comments of each reviewer below, and then summarise our planned revisions to the paper.

## Reviewer AyvP

_Restriction to single papers._ Nothing about our design restricts it to single papers, although for our
evaluation we have only considered excerpts from single papers. A multi-paper setup is an interesting use case
and one we will consider for future work.

_Checks for more complex claims cannot be generated with LLMs. Even for the simple cases, a human has to
verify the generated links._ Keeping a human in the loop for verification purposes seems important from a
trust point of view. For the simple cases, where it may seem unfortunate that author verification is still
needed, the key point here is that a small amount of author effort translates into a significant _reader_
benefit (since there are many more readers than authors). In future work we anticipate that automated testing
can reduce the author burden in this cases by automatically generating counterexamples.

More the more complex cases where current LLMs are unable to generate a full solution in one step, we envisage
a more interactive workflow similar to working with Copilot. some kind of hybrid system where the "easy" cases
are fully or semi-automated and the more complex cases are constructed by hand (with a Copilot-like assistant)
seems the most plausible.

## Planned revisions to paper

