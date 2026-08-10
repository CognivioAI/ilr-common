# Implementation Plan: Product Agent Expansion

## Overview

Transform the single-file `agents/product-agent.md` into a comprehensive multi-module directory `agents/product-agent/` following the established architect-agent pattern. Each module is a dedicated markdown file with its own title, responsibility, and operational instructions. The expanded Product Agent produces machine-readable YAML PRD output consumed by all downstream agents.

## Tasks

- [ ] 1. Set up directory structure and entry point
  - [ ] 1.1 Create `agents/product-agent/` directory and README.md entry point
    - Create the `agents/product-agent/` directory
    - Write `README.md` with master prompt, module index table (filename + one-line responsibility), workflow diagram (Mermaid), and agent invocation order
    - Include the cross-reference identifier prefix table (REQ-, US-, PER-, AI-, NFR-, RISK-, FEAT-)
    - Define the no-code-generation constraint and source-of-truth designation in the master prompt
    - _Requirements: 1.1, 1.2, 21.1, 21.2, 21.3, 21.4, 21.5, 22.3_

  - [ ] 1.2 Update legacy `agents/product-agent.md` with redirect notice
    - Replace all content in `agents/product-agent.md` with a redirect notice
    - State that the agent has moved to `agents/product-agent/`
    - Name `README.md` as the new entry point
    - _Requirements: 1.3_

- [ ] 2. Implement Core Discovery modules
  - [ ] 2.1 Create `agents/product-agent/vision.md`
    - Define vision statement capture (max 500 chars), mission (max 300 chars), success criteria (1-10 items)
    - Include target market (max 200 chars), value proposition (max 300 chars), differentiators (1-5, each max 150 chars)
    - Specify validation rules: reject submission if required fields are empty
    - Define success metric structure: numeric threshold, comparison operator (gt/lt/eq), unit, target date
    - Include change history tracking instructions
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 2.2 Create `agents/product-agent/business-analysis.md`
    - Define problem statement structure: affected users (1-200 chars), workaround (1-500 chars), frequency (Daily/Weekly/Monthly/Rare), business impact (10-500 chars)
    - Define pain point classification: severity (Critical/High/Medium/Low) and frequency
    - Define market opportunity structure: description (1-500 chars), estimated value (0.01-999999999.99), required investment (0.01-999999999.99)
    - Specify validation: reject if required fields empty or outside bounds
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ] 2.3 Create `agents/product-agent/personas.md`
    - Define persona structure: name, role, goals (1-5), pain points (1-5), proficiency (novice/intermediate/advanced/expert), permissions
    - Include user journey map: 3-10 touchpoints with stage, action, emotion (frustrated/neutral/satisfied/delighted)
    - Require persona-to-feature mapping (at least 1 feature per persona)
    - Specify validation: reject if required fields missing
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ] 2.4 Create `agents/product-agent/requirements-gathering.md`
    - Define requirement structure: domain, priority (Critical/High/Medium/Low), associated persona
    - Specify field validation: actor (min 2 chars), action (min 10 chars), expected outcome (min 10 chars)
    - Include conflict detection: flag contradictions with existing requirements by identifier
    - Specify rejection behavior: retain entered data for correction on validation failure
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 3. Checkpoint - Core Discovery modules
  - Ensure all core discovery module files are created and self-contained, ask the user if questions arise.

- [ ] 4. Implement Story & Criteria Generation modules
  - [ ] 4.1 Create `agents/product-agent/user-stories.md`
    - Define story format: "As a [persona]", "I want [goal]", "So that [benefit]" — all three non-empty
    - Include traceability: originating requirement ID and persona name
    - Define story point estimation: S (single component, no deps), M (2-3 components or 1 dep), L (4+ components or 2+ deps), XL (cross-system/new infra)
    - Specify error handling: return error if persona or discrete goal cannot be derived
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [ ] 4.2 Create `agents/product-agent/acceptance-criteria.md`
    - Define Given/When/Then (Gherkin) format for every user story
    - Require at least 1 happy path + 1 error scenario per story (max 10 total)
    - Require at least 2 edge case scenarios per story (empty inputs, max-length, invalid formats)
    - Specify error handling: reject if user story missing role, action, or benefit clause
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ] 4.3 Create `agents/product-agent/nfr.md`
    - Define 6 NFR categories: availability, performance, accessibility, security, compliance, privacy
    - Require category, quantified target, priority level, affected component(s)
    - Define performance thresholds: response time (1-60000ms), throughput (1-1000000 rps), error rate (0.0-100.0%)
    - Include accessibility WCAG level requirement (A/AA/AAA) with testable success criterion
    - Include availability uptime target (90.0-100.0%) and max downtime (0-43200 min/month)
    - Require security/compliance/privacy standard reference and data classification
    - Map each NFR to downstream agent(s)
    - Specify validation: reject if required fields missing
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_

- [ ] 5. Implement Strategy & Planning modules
  - [ ] 5.1 Create `agents/product-agent/prioritization.md`
    - Define MoSCoW classification: Must Have, Should Have, Could Have, Won't Have
    - Require justification: business value (1-10), effort (1-21 story points), risk (Low/Medium/High)
    - Define sort order: category rank ASC, business value DESC, effort ASC
    - Include re-prioritization tracking: previous category, new category, updated justification
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [ ] 5.2 Create `agents/product-agent/roadmap.md`
    - Define maturity levels: Idea, Discovery, MVP, Production, Enterprise with descriptions (max 500 chars)
    - Require entry criteria and exit criteria for each level
    - Define transition criteria between consecutive level pairs
    - Include dependency mapping: blocking/blocked features, max 10 direct deps per feature
    - Specify validation: reject level assignment if entry criteria not met
    - Specify circular dependency detection and rejection
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

  - [ ] 5.3 Create `agents/product-agent/competitor-analysis.md`
    - Define competitor profile: name, market position (leader/challenger/follower/niche), features (up to 20), strengths (up to 10), weaknesses (up to 10)
    - Support 1-20 competitor profiles
    - Define comparison matrix with capability status (fully available/partially available/not available)
    - Identify unique differentiators: our "fully available" vs all competitors "not available"
    - Require minimum 2 profiles before comparison/differentiator analysis
    - _Requirements: 11.1, 11.2, 11.3, 11.4_

  - [ ] 5.4 Create `agents/product-agent/release-planning.md`
    - Define sprint assignment based on priority, dependencies, and effort
    - Enforce capacity constraint: no sprint exceeds team capacity
    - Define critical path identification and 80% capacity warning
    - Include release criteria: required features, completion status, quality gate conditions
    - Specify error handling for unschedulable features
    - _Requirements: 17.1, 17.2, 17.3, 17.4_

  - [ ] 5.5 Create `agents/product-agent/backlog.md`
    - Define ordered list: up to 500 stories, unique numeric priority rank, status (New/Ready/In Progress/Done/Blocked), assigned sprint
    - Require reprioritization reason (1-500 chars, min 1 non-whitespace)
    - Specify rejection if reason not provided
    - Define stale detection: unchanged status + sprint for 2+ consecutive sprints
    - _Requirements: 18.1, 18.2, 18.3, 18.4_

- [ ] 6. Checkpoint - Strategy & Planning modules
  - Ensure all strategy and planning module files are created and self-contained, ask the user if questions arise.

- [ ] 7. Implement Experience & Flow modules
  - [ ] 7.1 Create `agents/product-agent/ui-ux.md`
    - Define user journey maps: sequential steps, decision points with branching, system responses, nominal + error paths
    - Define screen descriptions: component hierarchy, interactive/data-display components, data fields with domain model references, interaction types (click/text input/selection/gesture)
    - Define navigation flows: trigger conditions, target screens
    - Require loading state, empty state, and error state for each screen
    - _Requirements: 12.1, 12.2, 12.3, 12.4_

  - [ ] 7.2 Create `agents/product-agent/workflows.md`
    - Define user flows: step ID, step type (form/display/confirmation/redirect), route path, allowable next steps, decision points, terminal states
    - Specify data requirements per step: field names, data types, validation rules (required/min-length/max-length/pattern/custom)
    - Ensure output is complete for React Agent consumption without additional info
    - Specify structural validation: reject undefined next steps or cycles with no reachable terminal
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [ ] 8. Implement Intelligence & Measurement modules
  - [ ] 8.1 Create `agents/product-agent/ai-discovery.md`
    - Define 8 AI categories: Classification, Extraction, Validation, Recommendation, Chat, Translation, Summarization, Prediction
    - Trigger evaluation when requirement transitions to "approved" (within 30 seconds)
    - Assign confidence (0-100) and effort (Low/Medium/High) for categories scoring >= 20
    - Produce opportunity register ranked by value-to-effort ratio (confidence / effort_weight)
    - Handle "None Applicable" case with reasoning (min 20 chars)
    - Handle timeout: record "Evaluation Pending" and retry within 5 minutes
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [ ] 8.2 Create `agents/product-agent/analytics.md`
    - Define metrics per feature (1-10): name, numeric success threshold, direction (increase/decrease), timeframe (7-90 days)
    - Specify event implementation: event name, properties (1-20 with data types), trigger condition
    - Define measurement plan: link metrics to hypotheses with expected outcome and evaluation timeframe
    - Require falsifiable hypothesis; reject specification without one
    - _Requirements: 15.1, 15.2, 15.3, 15.4_

  - [ ] 8.3 Create `agents/product-agent/risk-analysis.md`
    - Define risk categories: Technical, Business, Schedule, Resource, Compliance
    - Require at least 1 risk per applicable category
    - Include risk structure: title (max 120 chars), description (max 500 chars), category, impact (1-5), probability (1-5), score (impact × probability)
    - Require mitigation (1-5 steps, each max 300 chars) and contingency for risks scoring > 9
    - Specify error handling for insufficient context data
    - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [ ] 9. Checkpoint - Experience, Flow, and Intelligence modules
  - Ensure all experience, flow, and intelligence module files are created and self-contained, ask the user if questions arise.

- [ ] 10. Implement Output & Quality modules
  - [ ] 10.1 Create `agents/product-agent/prd-output.md`
    - Define YAML 1.2 PRD generation instructions
    - Include complete PRD schema covering: metadata, vision, personas, requirements, user_stories, nfrs, priorities, ai_opportunities, risks, roadmap, quality_gate
    - Specify schema validation rules: all required fields present and non-empty
    - Define cross-reference system: REQ-001, US-001, PER-001, AI-001 prefixed identifiers that resolve within document
    - Include metadata: version (semver), maturity (Draft/In Review/Approved/Deprecated), last_updated (ISO 8601), author
    - Specify rejection behavior on validation failure with field-level errors
    - Include no-code-generation guardrails for PRD content
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 21.1, 21.4, 21.5_

  - [ ] 10.2 Create `agents/product-agent/product-review.md`
    - Define Product Review sub-agent completeness checks: stories without criteria, undefined personas, missing NFR categories
    - Define conflict detection: different outputs for same input/trigger/scenario
    - Define edge case identification: boundary conditions, unhandled errors, concurrent access
    - Specify review report format: severity (critical/major/minor), requirement ID, suggested resolution
    - Specify confirmation report for clean PRDs with timestamp
    - Define 60-second completion constraint
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5_

  - [ ] 10.3 Create `agents/product-agent/maturity-levels.md`
    - Define 5 maturity levels: Idea, Discovery, MVP, Production, Enterprise
    - Specify exit criteria per level (Idea: problem statement + persona; Discovery: validated need + metrics; MVP: requirements + criteria + prototype; Production: NFRs + runbook + monitoring; Enterprise: all Production + compliance + multi-tenant)
    - Define specification rigor per level
    - Enforce single-level-at-a-time transitions
    - Specify rejection on failed exit criteria with specific gaps listed
    - _Requirements: 23.1, 23.2, 23.3, 23.4_

  - [ ] 10.4 Create `agents/product-agent/downstream-integration.md`
    - Define Quality Gate checks: stories have criteria, edge cases documented, NFRs specified, dependencies identified, no open questions
    - Define handoff package: Machine_Readable_PRD, diagrams, open risks section
    - Specify halt behavior on failed quality gate with gap report
    - Tag artifacts with consuming agents (Architecture/Java/React/Test/DevOps)
    - Exclude artifacts not consumed by any agent
    - _Requirements: 24.1, 24.2, 24.3, 24.4, 24.5_

- [ ] 11. Implement Version History and Source of Truth enforcement
  - [ ] 11.1 Add version history tracking instructions to README.md
    - Define version history structure: version ID, timestamp, change description
    - Specify version increment and prior version recording on updates
    - Define single-spec-document reference pattern for all downstream agents
    - Include clarification response protocol: append to current spec version within 1 processing cycle
    - _Requirements: 22.1, 22.2, 22.3, 22.4_

- [ ] 12. Final checkpoint - Complete module verification
  - Ensure all 21 module files exist in `agents/product-agent/` and are self-contained
  - Verify README.md module index table lists all files with responsibilities
  - Verify workflow diagram reflects the complete invocation flow
  - Verify cross-reference prefix table is complete
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All implementation involves creating and editing markdown files — no executable code is generated
- Each module file must be self-contained with its own title, responsibility, and operational instructions
- The directory structure mirrors the established `agents/architect-agent/` pattern
- Cross-references use prefixed identifiers (REQ-, US-, PER-, AI-, NFR-, RISK-, FEAT-) for traceability
- The PRD schema in `prd-output.md` serves as the contract between Product Agent and all downstream agents
- Checkpoints ensure incremental validation of module groups
- Requirements R21 (no code generation) and R22 (source of truth) are cross-cutting and enforced in README.md master prompt

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "2.1", "2.2", "2.3", "2.4"] },
    { "id": 2, "tasks": ["4.1", "4.2", "4.3", "5.1", "5.2", "5.3", "5.4", "5.5"] },
    { "id": 3, "tasks": ["7.1", "7.2", "8.1", "8.2", "8.3"] },
    { "id": 4, "tasks": ["10.1", "10.2", "10.3", "10.4"] },
    { "id": 5, "tasks": ["11.1"] }
  ]
}
```
