# Key points to respond to

## AyvP (rating 6, confidence 3)

### Limited to single papers

I guess not actually true, but will need to think a bit about how best to respond.

### Checks for more complex claims cannot be generated with LLMs

Need to think about how we envisage this improving -- perhaps some kind of iterative refinement where LLM and human collaboratively build interpretation.

### Human has to verify all generated links anyway

Again, probably need to think about how we envisage this improving -- automated testing/validation?

## Bezq (rating 4, confidence 4 -- substantial AI content in the review)

### Re. subset of SciGen, could the authors provide more details on the selection logic, sampling criteria, and the exact number of examples used in evaluation?

How realistic would it be to cover all of SciGen, perhaps using the SuggestionAgent to generate the REPLACE tags?

### No analysis of statistical variance beyond standard deviation

We've already discussed improving this.

### Assess generalization to unseen writing styles or datasets

Not sure this makes sense -- we didn't train our system on SciGen.

### Can authors report statistics about the manual validation process? For examples, the ratio of accepted to rejected edits, average validation time per fragment, or the most common sources of rejection?

We might have to concede this point and say that we plan to do a proper user study as future work. The point of this study is to evaluate LLM competence at the basic problem.

The criticism that we may not reduce author workload but simply shift it from writing to debugging is worth responding to as part of this. I guess this applies to AI-generated code in general. There is probably some evidence that net productivity increases even if manual validation of machine-generated code is required.

### Could the authors conduct an ablation study removing or varying these helper components to determine how much of the system’s success derives from the LLM’s own reasoning versus predefined components (e.g. helper routines)?

I think we can do this. (We can also argue that having a fixed/standardised set of definitions might also be helpful, but I think their point remains.)

### The large performance gap between target-value-sharing (74.9%) and no-target (57.1%) suggests potential reliance on implicit answer leakage rather than actual LLM reasoning

Not sure I understand this point properly but one related thing we considered studying (and perhaps should in response to this) is what happens when the target value is incorrect. This is somewhat related to RQ2 and the ablation studies we considered for that.

## azjF (rating 3, confidence 3)

### More engineering-oriented, with relatively limited academic research value

Perhaps respond to this as part of respond to point below.

### Experimental design lacks comprehensive justification. Without comparisons to other baseline methods, it is difficult to determine whether this system represents the optimal solution (though, as a pioneering work, more in-depth ablation studies could be considered)

Maybe we have to concede this point and then propose more in-depth ablation studies.

## VRkd (rating 2, confidence 4)

### Isn't this supposed to be an HCI paper?

Yes, the next step of this work will go to CHI/UIST/IUI.

I think we need to argue that the technical contribution from an ML/NLP point of view is competency evaluation for LLMs.

### Since "human" is a really central part in this system, it would be nice to get evaluated by actual human/users too.

Let's respond to this as part of the question above.

### The evaluation, from an LLM/NLP standpoint, is perhaps not enough. It would be nice to show if the system could do well on some documented NLP tasks (like scientific literature?) to begin with.

I think this is more than we can plan to do for this version of the paper, but for a resubmission it might be worth looking at some of the NLP literature (with Fede's help). Unsurprisingly, there are well-known NLP problems that are highly relevant and could form part of the "capability set" required for a tool providing automated computational interpretation of a scientific paper. From a very quick glance, all of the following datasets (from the computational lingustics literature) are potentially relevant and might be of use for a future study:

SciERC (https://aclanthology.org/D18-1360/)
Qasper (https://aclanthology.org/2021.naacl-main.365/)
DROP (https://aclanthology.org/N19-1246/)
NUMGLUE (https://aclanthology.org/2022.acl-long.246.pdf)
SCIREX (https://aclanthology.org/2020.acl-main.670.pdf)
TAT-QA (https://aclanthology.org/2021.acl-long.254/)

(Fig. 1 in the TAT-QA paper looks very similar to some of ours.)

### Many of the niche details, like Table 1, are perhaps unnecessary for the main paper

Yes we wondered the same but in the end decided to be explicit about the kind of linguistic/quantitative competencies we wanted to evaluate because of the NLP component.
