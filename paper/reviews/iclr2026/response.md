# Response to reviewers

We thank our three reviewers for their useful feedback and suggestions for revising the paper. We address specific comments of each reviewer below, and then summarise how we plan to improve the paper.

## Reviewer AyvP

_Restriction to single papers._ Nothing about our design restricts it to single papers, although for our evaluation we have only considered excerpts from single papers. A multi-paper setup is an interesting use case and one we will consider for a real-world tool.

_Checks for more complex claims cannot be generated with LLMs. Even for the simple cases, a human has to verify the generated links._ Keeping a human in the loop for verification purposes seems important from a trust point of view. For the simple cases, where it may seem unfortunate that author verification is still needed, a small amount of author effort nevertheless translates into a significant _reader_ benefit (since there are many more readers than authors). In future work we anticipate providing automated testing as a way of reducing author burden by automatically exercising the generated code.

For the more complex cases, where current LLMs are unable to generate a full solution in one step, our current design should be able to form the basis of a more interactive workflow, similar to working with Copilot. For the present paper we are focused on exploring the limits of current LLM capabilities and validating a simpler architecture with less interaction.

## Reviewer Bezq

_Can authors report statistics about the manual validation process?_

A proper user study with the necessary experimental setup is out of scope for this work, but certainly something we plan to do as the next step, alongside implementing a more complete interactive workflow. For this paper, our main focus is on a basic agent-based architecture for interpretation synthesis and a solid evaluation of the required LLM competencies.

_Re. the subset of SciGen used, could you provide details on the selection logic/sampling criteria and exact number of examples used in the evaluation?_

For the submitted version we were only able to manually annotate 9 randomly-chosen SciGen issues (approximately 2% of the total), resulting in XX labelled replacement tasks. We have drawn a second (larger) random subsample (5% of the total) for manual annotation and will report on this in the final version of the paper. We have also added an automation pipeline to process every SciGen example using the Suggestion Agent, and will evaluate SuggestionAgent performance using manual annotations to estimate the noise rate. We are now able to evaluate the InterpretationAgent using the entirety of the SciGen data set, with confidence intervals adjusted to incorporate estimated noise.

_The framework rely on predefined helper routines such as trendWord or growShrink_. Could the authors conduct an ablation study removing or varying these helper components?

This is a good suggestion; we will do this and also consider other ablations studies (in addition to excluding the target string). It is worth emphasising that a fixed/standardised set of terminological definitions (for a given paper or research community, say) might be helpful (or even important in some domains, such as IPCC Summary for Policymaker reports), but your point remains valid.

_Large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning._

## Reviewer VRkd

> Many of the niche details, like Table 1, are perhaps unnecessary for the main paper

We would like to keep Table 1 in the body of the paper to be explicit about the specific linguistic/quantitative LLM competencies we evaluate in the paper. However it may be possible to streamline the figure a bit. We will also make a pass over the paper for other details that might be spurious for our audience here.

## Planned revisions to paper

- Clarify manually-annotated subsets of SciGen are randomly chosen; add additional manual annotations to grow subset from 2% to 5%. We will include this data set as supplementary material and release it as an open source benchmark.
- Evaluate SuggestionAgent performance against manual annotations; estimate noise in SuggestionAgent labelling.
- Extend our correctness evaluation (Fig. 6(b)) to full SciGen dataset, using SuggestionAgent annotations (and adjusted for noise)
