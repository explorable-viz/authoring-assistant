[Post each of the top-level sections below a separate comment, with title as given]

# General Response to Reviewers

We thank all reviewers for their time and expertise in evaluating our submission. We are grateful for the positive feedback, including the recognition that the proposed system addresses an important research direction (Reviewers AyvP, Bezq and VRkd), that the connection between LLM-based code synthesis and data provenance systems is novel (Reviewer Bezq), and that our experimental results, while in need of some further work, indicate that the approach is feasible in practice (Reviewer azjF). The constructive feedback provided by the reviewers has been invaluable in understanding how to improve the evaluation. We have provided detailed responses to each reviewer individually and outline below how we plan to further revise the paper, including the new experiments conducted in response to multiple reviewer requests.

## Planned Revisions and New Experiments

- Clarify manually-annotated subsets of SciGen are randomly chosen; add additional manual annotations to grow subset from 2% to 5%. We will include this data set as supplementary material and also plan to release it as an open source benchmark.
- Evaluate SuggestionAgent performance against manual annotations; estimate noise in SuggestionAgent labelling.
- Extend our correctness evaluation (Fig. 6(b)) to full SciGen dataset, using SuggestionAgent annotations (and adjusted for noise)
- Expand the treatment of RQ2

# Response to Reviewer AyvP

> - The proposed system still seems to be in early stages with respect to what can be verified, which limits its usefulness in practice. First, as far as I understand, it is limited to single papers and does not allow to check claims a paper makes about results presented in another paper. This is where a system like the proposed would be most useful

Thank you for the suggestion; a multi-paper setup is indeed an interesting use case. Although for our evaluation we only considered excerpts from single papers, nothing about our design restricts it to such, so we will consider this for a real-world tool.

> - Second, the claims that can be checked seem to be quite simple (checks for more complex claims cannot be generated with LLMs) and thus easy to check manually. Even then, a human has to verify all generated links, which in at least some cases are wrong. This begs the question of whether a manual system where only larger and more important claims are annotated would not be more useful in practice.

Keeping a human in the loop for verification purposes seems important from a trust point of view. For the simple cases, where it may seem unfortunate that author verification is still needed, a small amount of author effort nevertheless translates into a significant _reader_ benefit (since there are many more readers than authors). In future work we anticipate providing automated testing as a way of reducing author burden by automatically exercising the generated code.

For the more complex cases, where current LLMs are unable to generate a full solution in one step, our current design should be able to form the basis of a more interactive workflow, similar to working with Copilot. For the present paper we are focused on exploring the limits of current LLM capabilities and validating a simpler architecture with less interaction.

# Response to Reviewer Bezq

> - Although the paper provides useful category-level statistics, it does not analyze statistical variance beyond reporting standard deviation or assess generalization to unseen writing styles or datasets.

We have changed our figures now use box plots to provide a better sense of the underlying distribution, and annotated with number of samples; thanks for the suggestion.

In our experiments the LLM was not fine-tuned or prompted with SciGen examples other than as part of the testing step, so all the SciGen examples were acting as out-of-distribution problems. We will make sure this is clear.

> - Can authors report statistics about the manual validation process? For examples, the ratio of accepted to rejected edits, average validation time per fragment, or the most common sources of rejection? Such data would illustrate how practical and scalable the workflow is for real authors.

Thank you for this point. A proper user study with the necessary experimental setup is out of scope for this work, but certainly something we plan to do as the next step, alongside implementing a more complete interactive workflow; we recognise there is a risk (as with all AI-generated code) of simply shifting author workload from writing to debugging. For this paper, our main focus is on a basic agent-based architecture for interpretation synthesis and a solid evaluation of the required LLM competencies.

> - The paper states that experiments were conducted on “a subsample of the SciGen dataset” (line 367), but it remains unclear how this subset was chosen. Could the authors provide more details on the selection logic, sampling criteria, and the exact number of examples used in evaluation?

For the submitted version we were only able to manually annotate 9 randomly-chosen SciGen issues (approximately 2% of the total), resulting in XX labelled replacement tasks. We have drawn a second (larger) random subsample (5% of the total) for manual annotation and will report on this in the final version of the paper. We have also added an automation pipeline to process every SciGen example using the Suggestion Agent, and will evaluate SuggestionAgent performance using manual annotations to estimate the noise rate. We are now able to evaluate the InterpretationAgent using the entirety of the SciGen data set, with confidence intervals adjusted to incorporate estimated noise.

> - It seems this framework rely on predefined helper routines such as trendWord or growShrink, which encode semantic logic that the model merely invokes rather than learns. Moreover, the large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning. Could the authors conduct an ablation study removing or varying these helper components to determine how much of the system’s success derives from the LLM’s own reasoning versus predefined components?

This is a good suggestion; we will do this and also consider other ablations studies (in addition to excluding the target string). It is worth emphasising that adopting a fixed or standardised set of terminological definitions (for a given paper or research community, say) might be helpful (or even important in some domains, such as IPCC Summary for Policymaker reports), but your point remains valid.

Re. the target sharing point, the target string is an important part of the query for the important use case where the reader or author is trying to retrofit an interpretation to text already written. So this probably doesn't qualify as answer leakage per se. Your point about determining when LLM reasoning is actually happening a good one though; counterfactual testing, which is possible only given a hand-generated solution, is designed to reveal this. We will improve our reporting on this (RQ2).

# Response to Reviewer azjF

> - The system presented in this paper is more engineering-oriented, with relatively limited academic research value.
> - Overall, it can be regarded as a well-executed engineering paper.

We agree that it is an engineering-oriented paper, and thank you for the positive evaluation in that respect. However we also address important research questions around how such an agent-based tool might be designed, how current LLM competencies meet the needs of such a design, and how manual and automatic verification can be integrated and assessed.

> - The experimental design lacks a comprehensive justification. Without comparisons to other baseline methods, it is difficult to determine whether this system represents the optimal solution (though, as a pioneering work, more in-depth ablation studies could be considered).

The lack of directly related prior work does indeed make comparison to baseline methods difficult. We agree more in-depth ablation studies are needed; we will investigate removing the predefined helper routines (see response to reviewer Bezq above), along with other other similar experiments, and include those in our reporting on RQ1 and RQ2.

# Response to Reviewer VRkd

> - My greatest confusion is: isn't this supposed to be an HCI paper? Shouldn't some version of this go to CHI/CSCW/UIST/...? To me, there isn't any technical contribution for an ML venue beyond getting the system implemented, but this type of contribution should be very suitable for the HCI venues.

The natural next step of this work will be a Copilot-like full-featured tool, which will enable a full user study, and which would likely go to venue like CHI or IUI. For this paper, our aim to validate key components of such a tool, including:

- competency evaluation for LLMs (which we will substantially improve for this paper)
- proof-of-concept agentic architecture, including support for compiler-in-the-loop and manual validation (both important for this task)

We believe that this is of interest to the ML community, at least from NLP and tool architecture points of view, although we concede that the evaluation in the submitted version of the paper is in need further work. We have discussed elsewhere how we plan to address that in the final revision.

> - Since "human" is a really central part in this system, it would be nice to get evaluated by actual human/users too.

Thank you for emphasising the importance of this. We agree, but unfortunately this is not within ascope for this paper, not least because it would require a more mature tool, as discussed above.

> - The evaluation, from an LLM/NLP standpoint, is perhaps not enough. It would be nice to show if the system could do well on some document NLP tasks (like scientific literature?) to begin with.

There are several well-known NLP problems, beyond argument mining and related techniques already discussed in the paper, that are highly relevant and could form part of the capabilityset" required for a tool providing "automated interpretation" of a scientific paper. From the computational lingustics literature, data sets like [SciERC](https://aclanthology.org/D18-1360/), [SCIREX](https://aclanthology.org/2020.acl-main.670.pdf) and [Qasper](https://aclanthology.org/2021.naacl-main.365/) (among others) look relevant, and [TAT-QA](https://aclanthology.org/2021.acl-long.254/) in particular looks closely related to some of our natural language tasks. We will add some discussion around this to both Related Work and Future Work, and will revisit it as we move towards a more mature tool.

> Many of the niche details, like Table 1, are perhaps unnecessary for the main paper, when under consideration at an ML venue.

We prefer to keep Table 1 in the body of the paper, to be explicit about the specific linguistic/quantitative LLM competencies evaluated in the paper; however it should be possible to streamline the figure a bit. We will also make a pass over the paper for other details that might be spurious for the ICLR audience.
