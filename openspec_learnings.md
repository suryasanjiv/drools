# Learings

## Intro

OpenSpec is a framework for Spec Driven Development. It acts a SDD playbook for the agent to enable sustainable AI assisted development by driving repeatable outputs from the LLM and documenting the specs.

## The Artifacts and Workflow

### Project Structure

        │
        ├── openspec/
        │   ├── changes/
        │   │   └── add-travel-policy-rules/
        │   │       ├── proposal.md
        │   │       ├── design.md
        │   │       ├── tasks.md
        │   │       └── specs/
        │   │           ├── booking/
        │   │           │   └── spec.md
        │   │           └── approvals/
        │   │               └── spec.md
        │   │
        │   └── specs/
        │       ├── booking/
        │       │   └── spec.md
        │       ├── approvals/
        │       │   └── spec.md
        │       └── policy-engine/
        │           └── spec.md
        │
        ├── src/
        │   └── ...
        │

### Artifacts

Create artifacts to :

- provide context for the LLM
- verify the implementation against
- document code

Default artifacts:

- [Proposal](https://github.com/Fission-AI/OpenSpec/blob/main/schemas/spec-driven/templates/proposal.md): high level; what, how, capabilities, impact
- [Design](https://github.com/Fission-AI/OpenSpec/blob/main/schemas/spec-driven/templates/design.md): provide the technical details + trade offs
- [Spec](https://github.com/Fission-AI/OpenSpec/blob/main/schemas/spec-driven/templates/spec.md): provide the specs in Behavior Driven Development style language (when this, then do that); aims to be specific so may be not as business friendly as a user story; in terms of delta (added, modified, removed); specs get merged into a final set
- [Task](https://github.com/Fission-AI/OpenSpec/blob/main/schemas/spec-driven/templates/spec.md): provide trackable steps the agent will use to implement change

Dependency flow:

Proposal
/ \
 v v
Design Specs
\ /
v v
Task


### Workflow

The fast flow:
- `/opsx:propose`: all in one workflow-
- less request, prememium request efficient
- faster
- risk in having docs that do not match your intent 


```
/opsx:explore ──► 
/opsx:propose ──► 
/opsx:apply ──► 
/opsx:archive
```


The slow flow:
- create each artifact one by one
- less request, prememium request efficient
- faster
- risk in having docs that do not match your intent 
```
/opsx:explore ──► /
opsx:new ──► 
/opsx:continue ──► 
...
continue to create each artifact one by one
...
/opsx:continue ──►  
/opsx:apply ──► 
/opsx:archive
```

`opsx-explore`: use to explore new ideas before going into artifact writing
''

## Additional Documentation
RFC 2119 keywords (SHALL, MUST, SHOULD, MAY) communicate intent:

MUST/SHALL — absolute requirement
SHOULD — recommended, but exceptions exist
MAY — optional


## Supporting Screenshots
