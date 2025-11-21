# Response to reviewers

We thank our three reviewers for their useful feedback and suggestions for revising the paper. We address specific comments of each reviewer below, and then summarise how we plan to improve the paper.

## Reviewer AyvP

_Restriction to single papers._ Nothing about our design restricts it to single papers, although for our evaluation we have only considered excerpts from single papers. A multi-paper setup is an interesting use case and one we will consider for a real-world tool.

_Checks for more complex claims cannot be generated with LLMs. Even for the simple cases, a human has to verify the generated links._ Keeping a human in the loop for verification purposes seems important from a trust point of view. For the simple cases, where it may seem unfortunate that author verification is still needed, a small amount of author effort nevertheless translates into a significant _reader_ benefit (since there are many more readers than authors). In future work we anticipate providing automated testing as a way of reducing author burden by automatically exercising the generated code.

For the more complex cases, where current LLMs are unable to generate a full solution in one step, our current design should be able to form the basis of a more interactive workflow, similar to working with Copilot. For the present paper we are focused on exploring the limits of current LLM capabilities and validating a simpler architecture with less interaction.

## Reviewer Bezq

_Can authors report statistics about the manual validation process?_

A proper user study with the necessary experimental setup is out of scope for this work, but certainly something we plan to do as the next step, alongside implementing a more complete interactive workflow; we recognise there is a risk (as with all AI-generated code) of simply shifting author workload from writing to debugging. For this paper, our main focus is on a basic agent-based architecture for interpretation synthesis and a solid evaluation of the required LLM competencies.

_Re. the subset of SciGen used, could you provide details on the selection logic/sampling criteria and exact number of examples used in the evaluation?_

For the submitted version we were only able to manually annotate 9 randomly-chosen SciGen issues (approximately 2% of the total), resulting in XX labelled replacement tasks. We have drawn a second (larger) random subsample (5% of the total) for manual annotation and will report on this in the final version of the paper. We have also added an automation pipeline to process every SciGen example using the Suggestion Agent, and will evaluate SuggestionAgent performance using manual annotations to estimate the noise rate. We are now able to evaluate the InterpretationAgent using the entirety of the SciGen data set, with confidence intervals adjusted to incorporate estimated noise.

_The framework rely on predefined helper routines such as trendWord or growShrink_. Could the authors conduct an ablation study removing or varying these helper components?

This is a good suggestion; we will do this and also consider other ablations studies (in addition to excluding the target string). It is worth emphasising that a fixed/standardised set of terminological definitions (for a given paper or research community, say) might be helpful (or even important in some domains, such as IPCC Summary for Policymaker reports), but your point remains valid.

_Large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning._

The target string is present in one of our main use cases (where the reader or author is trying to retrofit an interpretation to text already written), so this doesn't qualify as answer leakage per se. Counterfactual testing, which is possible for manually annotated solutions, exposes whether some kind of computational reasoning has actually happened. We will improve our reporting on this (RQ2).

To respond to your additional points:

_No analysis of statistical variance beyond standard deviation._

Our figures now use box plots for a better sense of the underlying distribution, annotated with the number of samples.

_Assess generalization to unseen writing styles or datasets._

In our experiments the LLM was not fine-tuned or prompted with SciGen examples other than as part of the test cases themselves, so all the SciGen examples were acting as out-of-distribution problems.

## Reviewer azjF

_More engineering-oriented, with relatively limited academic research value._ It is an engineering-oriented paper, however we address important research questions around how such an agent-based tool might be designed, how currently LLM competencies meet the needs of such a design, and how manual and automatic verification can be integrated and assessed.

_Without comparisons to other baseline methods, it is difficult to determine whether this system represents the optimal solution (though, as a pioneering work, more in-depth ablation studies could be considered)._ The lack of directly related prior work does indeed make comparison to baseline methods difficult. We agree more in-depth ablation studies would be appropriate; we will investigate removing the predefined helper routines (see response to reviewer Bezq above), along with other other similar experiments, and include those in our reporting on RQ1 and RQ2.

## Reviewer VRkd

_Isn't this supposed to be an HCI paper? To me, there isn't any technical contribution for an ML venue beyond getting the system implemented. Since "human" is a really central part in this system, it would be nice to get evaluated by actual human/users too._ The natural next step of this work will include a Copilot-like full-featured tool, which will enable a full user study. And indeed this would likely go to venue like CHI or IUI. For this paper, our aim to validate key components of such a tool, including:

- competency evaluation for LLMs (which we will substantially improve for this paper)
- proof-of-concept agentic architecture, including support for compiler-in-the-loop and manual validation (both important for this task)

We believe that this is of interest to the ML community, at least from NLP and tool architecture points of view, although we concede that the evaluation in the submitted version of the paper was in need further work. We discuss elsewhere how we plan to address that in the final revision.

### The evaluation, from an LLM/NLP standpoint, is perhaps not enough. It would be nice to show if the system could do well on some documented NLP tasks (like scientific literature?) to begin with.

_Many of the niche details, like Table 1, are perhaps unnecessary for the main paper._ We prefer to keep Table 1 in the body of the paper, to be explicit about the specific linguistic/quantitative LLM competencies evaluated in the paper; however it should be possible to streamline the figure a bit. We will also make a pass over the paper for other details that might be spurious for the ICLR audience.

## Planned revisions to paper

- Clarify manually-annotated subsets of SciGen are randomly chosen; add additional manual annotations to grow subset from 2% to 5%. We will include this data set as supplementary material and release it as an open source benchmark.
- Evaluate SuggestionAgent performance against manual annotations; estimate noise in SuggestionAgent labelling.
- Extend our correctness evaluation (Fig. 6(b)) to full SciGen dataset, using SuggestionAgent annotations (and adjusted for noise)
- Expand the treatment of RQ2
