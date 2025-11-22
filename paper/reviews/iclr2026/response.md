[Post each of the top-level sections below as a separate comment, with title as given]

# General Response to Reviewers

We thank all reviewers for their time and expertise in evaluating our submission. We are grateful for the positive feedback; we are glad to hear that the proposed system addresses an important research direction (Reviewers AyvP, Bezq and VRkd), that combining LLM-based code synthesis with data provenance systems seems to be a novel approach to the problem (Reviewer Bezq), and that our experimental results suggest that the approach is practical (Reviewer azjF). The critical feedback provided by the reviewers has also been invaluable in understanding how to improve various aspects of the empirical study and analysis. We have provided detailed responses to each reviewer separately; below we outline how we plan to further revise the paper, including new experiments conducted in response to reviewer suggestions.

## Planned Revisions and New Experiments

### Improvements to RQ1 study

We are implementing the following improvements to the RQ1 dataset and evaluation:

- We are adding additional manual annotations to substantially increase the size of the hand-labelled subset (now 5% of SciGen). We will clarify that the subset was chosen by randomly choosing from each linguistic category.
- The SelectionAgent now also performs an initial labelling by linguistic category, in addition to identifying phrases to be replaced. We are evaluating this against the manual annotations to estimate the noise in the automated labelling.
- This allows us to derive an annotated dataset based on the full SciGen dataset, which we call SciGen-interpret. We will include this dataset as supplementary material, and release as an open source benchmark alongside the final version of the paper.
- We will redo the RQ1 study using the full dataset (adjusting confidence intervals for noise); this should remove some degenerate estimates (e.g. success rates of 0% and 100%) attributable to sparsity in the original subset.
- We will perform an additional ablation study to assess the contribution of predefined helpers to success rates.

### Improvements to RQ2 study

RQ2 (counterfactual testing) was given only a cursory treatment in the submitted version. We are implementing the following improvements:

- Using the improved hand-labelled dataset, redo the RQ2 study of whether LLM solutions which are correct (in the sense of evaluating to the target strong) are also counterfactually correct (i.e. generalise under perturbations of the data).
- Report on counterfactual robustness (omitted from submitted version), namely the mean proportion of responses that are both correct and counterfactually correct, disaggregated by linguistic category.

# Response to Reviewer AyvP

> - The proposed system still seems to be in early stages with respect to what can be verified, which limits its usefulness in practice. First, as far as I understand, it is limited to single papers and does not allow to check claims a paper makes about results presented in another paper. This is where a system like the proposed would be most useful -- while directly linking to evidence in the same paper is useful, manually checking this information is not nearly as laborious as checking something in another paper.

Thank you for the suggestion; a multi-paper setup is indeed an interesting use case. For our evaluation we have only considered excerpts from single papers, but nothing about our design restricts it to such, so we will consider this for a real-world tool.

> - Second, the claims that can be checked seem to be quite simple (checks for more complex claims cannot be generated with LLMs) and thus easy to check manually. Even then, a human has to verify all generated links, which in at least some cases are wrong. This begs the question of whether a manual system where only larger and more important claims are annotated would not be more useful in practice.

Keeping a human in the loop for verification purposes seems important from a trust point of view. Although for simple cases it may seem unfortunate that author verification is still needed, it is important to emphasise that a small amount of author effort nevertheless translates into a significant _reader_ benefit, since there are many more readers than authors. It is also worth emphasising that "debugging" by simply interacting with the output is less technically demanding than using a traditional debugger. In future work we anticipate supporting authors better via automated testing, for example by synthesising counterfactual test cases which highlight potential problems automatically.

For the more complex cases, where current LLMs are unable to generate a full solution in one step, our current design should be able to form the basis of a more interactive workflow, similar to working with Copilot, where users iteratively refine the goal by nudging, re-prompting, and constraining via partial code. For the present paper we are focused on exploring the limits of current LLM capabilities and validating a simpler agent-based architecture where user interaction is restricted mainly to validation rather than iterative refinement.

# Response to Reviewer Bezq

> - Although the paper provides useful category-level statistics, it does not analyze statistical variance beyond reporting standard deviation or assess generalization to unseen writing styles or datasets.

We have changed our figures to use box plots to provide a better sense of the underlying distribution, additionally annotated with number of samples; we appreciate the comment.

In our experiments the LLM was not fine-tuned or prompted with SciGen examples other than as part of the testing step, so all the SciGen examples were acting as out-of-distribution problems. We will make sure this is clear.

> - Can authors report statistics about the manual validation process? For examples, the ratio of accepted to rejected edits, average validation time per fragment, or the most common sources of rejection? Such data would illustrate how practical and scalable the workflow is for real authors.

Thank you for this point. A proper user study with the necessary experimental setup is out of scope for this work, but something we plan to do as the next step, alongside implementing a more complete interactive workflow. (We certainly recognise that there is a risk, as with all AI-generated code, of simply shifting author workload from writing to debugging/validation.) For this paper, our main focus is on a basic agent-based architecture for interpretation synthesis, plus a solid evaluation of the required LLM competencies.

> - The paper states that experiments were conducted on “a subsample of the SciGen dataset” (line 367), but it remains unclear how this subset was chosen. Could the authors provide more details on the selection logic, sampling criteria, and the exact number of examples used in evaluation?

For the submitted version we were only able to manually annotate 9 randomly-chosen SciGen issues (approximately 2% of the total), resulting in only 56 labelled replacement tasks for that dataset. We have since selected a second (larger) random subsample (5% of the total) for manual annotation and will report on this in the final version of the paper. We have also added an automation pipeline to process every SciGen example using the Suggestion Agent, and will evaluate those annotations relative to the manual baseline; this means we can now evaluate the core InterpretationAgent on the entirety of the SciGen dataset (with confidence intervals adjusted to incorporate estimated noise).

> - It seems this framework rely on predefined helper routines such as trendWord or growShrink, which encode semantic logic that the model merely invokes rather than learns. Moreover, the large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning. Could the authors conduct an ablation study removing or varying these helper components to determine how much of the system’s success derives from the LLM’s own reasoning versus predefined components?

This is a good suggestion; we will do this and also consider other ablations studies (in addition to excluding the target string). It is worth pointing out that fixed or standardised terminological definitions (for a given paper or research community, say) might be helpful or even important in some domains; for example the IPCC Summary for Policymaker reports use terms like _extremely likely_ in a specific predefined ways. Nevertheless your general point remains valid.

Re. the target-sharing point, the target string is an important part of the query for the key use case where the reader or author is trying to retrofit an interpretation to text already written. So this probably does not qualify as answer leakage per se. Your point about determining whether LLM reasoning is actually happening is important though; counterfactual testing, which is possible only given a hand-generated solution, is precisely designed to catch this. We will improve our reporting on this (RQ2).

# Response to Reviewer azjF

> - The system presented in this paper is more engineering-oriented, with relatively limited academic research value.
> - Overall, it can be regarded as a well-executed engineering paper.

We agree that it is an engineering-oriented paper, and appreciate your positive evaluation in that respect. However we also address important research questions around how such an agent-based tool might be designed, how current LLM competencies meet the needs of such a design, and how manual and automatic verification can be integrated and assessed.

> - The experimental design lacks a comprehensive justification. Without comparisons to other baseline methods, it is difficult to determine whether this system represents the optimal solution (though, as a pioneering work, more in-depth ablation studies could be considered).

The lack of directly related prior work does indeed make comparison to baseline methods difficult. We agree more in-depth ablation studies are needed; we will investigate removing the predefined helper routines (see response to reviewer Bezq above), along with other similar experiments, and include those in our reporting on RQ1 and RQ2.

# Response to Reviewer VRkd

> - My greatest confusion is: isn't this supposed to be an HCI paper? Shouldn't some version of this go to CHI/CSCW/UIST/...? To me, there isn't any technical contribution for an ML venue beyond getting the system implemented, but this type of contribution should be very suitable for the HCI venues.

The natural next step of this work will be a full-featured Copilot-like authoring (or reading) tool, which will enable a full user study. This will likely go to a venue like CHI, UIST, or IUI. For this paper, our aim to validate key components of such a tool, including:

- competency evaluation for LLMs (which we will substantially improve for this paper)
- proof-of-concept agentic architecture, including support for compiler-in-the-loop and manual validation (both important for this task)

We believe that this is of interest to the ML community, at least from NLP and tool architecture points of view, although we concede that the evaluation in the submitted version of the paper is in need further work. We have discussed elsewhere how we are addressing that for the final revision.

> - Since "human" is a really central part in this system, it would be nice to get evaluated by actual human/users too.

We agree, but unfortunately this is not within scope for this paper, not least because it would require a more mature tool implementation, as discussed above.

> - The evaluation, from an LLM/NLP standpoint, is perhaps not enough. It would be nice to show if the system could do well on some document NLP tasks (like scientific literature?) to begin with.

Thank you for highlighting this. There are indeed several well-known NLP problems, beyond argument mining and related techniques already discussed in the paper, which could form part of the "capability set" required for a tool like the one we have in mind. From the computational lingustics literature, datasets like [SciERC](https://aclanthology.org/D18-1360/), [SCIREX](https://aclanthology.org/2020.acl-main.670.pdf) and [Qasper](https://aclanthology.org/2021.naacl-main.365/) (among others) have some relevance, and [TAT-QA](https://aclanthology.org/2021.acl-long.254/) has problems that look closely related to some of our natural language tasks. We will add some discussion around these to Related Work and consider how some of these challenge problems would become relevant in a more mature tool.

> Many of the niche details, like Table 1, are perhaps unnecessary for the main paper, when under consideration at an ML venue.

We prefer to keep Table 1 in the body of the paper, to be explicit about the specific linguistic/quantitative LLM competencies evaluated in the paper; however it should be possible to streamline the figure a bit. We will also make a pass over the paper for other details that might be spurious for the ICLR audience.
