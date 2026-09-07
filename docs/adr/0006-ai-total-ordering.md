# The model ranks every plannable task; Java selects what fits

The AI returns a total ordering of every plannable task. The scheduler places must-include work, then walks the optional ranking until day capacity is gone. Validation checks known ids, completeness, uniqueness, and block order — not leftover arithmetic. Keeping the model in charge of the optional subset would preserve the class of "the model did arithmetic wrong" failures 1.2.0 is meant to end. Dropping the model entirely would reopen a roadmap decision that ranking stays with the AI.
