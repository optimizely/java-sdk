---
name: create-pr-current
displayName: Create PR for Current Branch
description: Creates a pull request for the current branch in java-sdk repository. Must be explicitly invoked with /create-pr-current to avoid confusion with create-prs agent.
version: 1.0.0
disable-model-invocation: true
requiredTools:
  - Bash
  - Read
  - mcp__github__create_pull_request
---

# Create PR for Current Branch

Creates a pull request for the current branch in the java-sdk repository.

## Instructions

When invoked, follow these steps:

**🚨 CRITICAL: Before starting the workflow, use TodoWrite to set up all steps as pending todos.**

**Step Setup (Use TodoWrite tool immediately):**
```
1. Get current branch information
2. Check for merge conflicts with master branch
3. Get repository information
4. Push current branch
5. Generate PR title and description
6. Create pull request
7. Report results
```

**TodoWrite Setup Example:**
```json
{
  "todos": [
    {
      "content": "Get current branch information",
      "activeForm": "Getting current branch information",
      "status": "pending"
    },
    {
      "content": "Check for merge conflicts with master branch",
      "activeForm": "Checking for merge conflicts with master branch",
      "status": "pending"
    },
    {
      "content": "Get repository information",
      "activeForm": "Getting repository information",
      "status": "pending"
    },
    {
      "content": "Push current branch",
      "activeForm": "Pushing current branch",
      "status": "pending"
    },
    {
      "content": "Generate PR title and description",
      "activeForm": "Generating PR title and description",
      "status": "pending"
    },
    {
      "content": "Create pull request",
      "activeForm": "Creating pull request",
      "status": "pending"
    },
    {
      "content": "Report results",
      "activeForm": "Reporting results",
      "status": "pending"
    }
  ]
}
```

**After completing each step, use TodoWrite to mark it as completed before proceeding to the next step.**

---

### 1. Get Current Branch Information
**Mark this step as in_progress using TodoWrite**
- Use `git branch --show-current` to get the current branch name
- Use `git status` to verify there are no uncommitted changes
- If uncommitted changes exist, warn user and ask if they want to commit first

**Mark Step 1 as completed using TodoWrite before proceeding**

---

### 2. Check for Merge Conflicts with Master Branch
**Mark this step as in_progress using TodoWrite**

#### a. Detect Potential Conflicts (Physical and Logical)
- Fetch latest master: `git fetch origin master`
- Check for physical conflicts: `git merge-tree $(git merge-base HEAD origin/master) HEAD origin/master`
- Check for .md file changes: `git diff --name-only origin/master...HEAD | grep '\.md$'`
- **Decision logic**:
  - If .md files changed → ALWAYS proceed to conflict analysis (even if no physical conflicts)
    - Reason: Logical conflicts in agent configs/prompts cannot be detected by git
  - If only non-.md files changed AND no physical conflicts → Skip to step 3
  - If physical conflicts detected → Proceed to conflict analysis

#### b-h. Resolve Conflicts with Semantic Evaluation

**🚨 CRITICAL: Follow the detailed process in [prompts/rules/git-logical-merge.md](../../prompts/rules/git-logical-merge.md)**

This process handles both physical conflicts (detected by git) and logical conflicts (semantic incompatibilities in .md files that git cannot detect).

**The process includes these critical steps:**
- **Step b**: Analyze files with semantic evaluation (understand what EACH side added)
- **Step c**: Categorize conflict severity (CRITICAL/HIGH/MEDIUM/LOW)
- **Step d**: Present critical conflicts to user with impact assessment
- **Step e**: **BLOCKING** - Use AskUserQuestion tool for user decision (MANDATORY)
- **Step f**: Execute resolution strategy based on user choice
- **Step g**: **MANDATORY** - Verify resolution preserves intent using Read tool
- **Step h**: **GATE CHECK** - Pre-commit checklist (all answers must be YES)

**⚠️ You MUST follow every step in git-logical-merge.md. Do NOT skip steps e, g, or h - they contain blocking requirements and verification gates.**

See [git-logical-merge.md](../../prompts/rules/git-logical-merge.md) for complete step-by-step instructions with examples.

**Mark Step 2 as completed using TodoWrite before proceeding**

---

### 3. Get Repository Information
**Mark this step as in_progress using TodoWrite**
- Repository owner: Extract from git remote (e.g., "optimizely")
- Repository name: "java-sdk"
- Base branch: Typically "master" (verify with `git remote show origin | grep "HEAD branch"`)

**Mark Step 3 as completed using TodoWrite before proceeding**

---

### 4. Push Current Branch
**Mark this step as in_progress using TodoWrite**
- Push branch to remote: `git push -u origin <branch-name>`
- Verify push succeeded

**Mark Step 4 as completed using TodoWrite before proceeding**

---

### 5. Generate PR Title and Description
**Mark this step as in_progress using TodoWrite**

#### PR Title (Local Rule - java-sdk Repository)
- **Format:** `[TICKET-ID] Brief description of changes`
- **Ticket ID:** Use uppercase format (e.g., `[FSSDK-12345]`) or `[FSSDK-0000]` if no ticket
- **Example:** `[FSSDK-12345] Add feature rollout support`

#### PR Body/Description (Follow pr-format.md)

**🚨 CRITICAL WORKFLOW - Follow these steps exactly:**

**Step 1: Read the Template**
- ALWAYS read `prompts/rules/pr-format.md` first (lines 55-70 for template)
- The template has ONLY these sections:
  - ## Summary
  - ## Changes
  - ## Jira Ticket
  - ## Notes (Optional - only for breaking changes)

**Step 2: Read Forbidden Sections**
- Read pr-format.md lines 48-53 for what NOT to include
- NEVER add: Testing, Test Coverage, Quality Assurance, Files Modified, Implementation Details
- NEVER add: Commits section (similar to Files Modified)

**Step 3: Generate PR Description**
- Use ONLY the template sections from pr-format.md
- Summary: 2-3 sentences max
- Changes: 3-5 bullet points, high-level only
- Jira Ticket: `[FSSDK-0000](https://optimizely-ext.atlassian.net/browse/FSSDK-0000)` or actual ticket
- Notes: Only if breaking changes exist

**Step 4: Verify Before Sending**
- Check: Does output have exactly Summary, Changes, Jira Ticket (and optionally Notes)?
- Check: Does output have ANY sections not in the template (Commits, Files, Tests, etc.)?
- If ANY extra sections exist → REMOVE THEM
- Only proceed when output matches template exactly

**Mark Step 5 as completed using TodoWrite before proceeding**

---

### 6. Create or Update Pull Request
**Mark this step as in_progress using TodoWrite**

#### a. Check if PR already exists
- Use `mcp__github__list_pull_requests` with `head` parameter to check for existing PR
- Search for PRs with head branch matching current branch

#### b. If PR exists - Update it
- Use `mcp__github__update_pull_request` with PR number
- Parameters:
  - `owner`: Repository owner
  - `repo`: "java-sdk"
  - `pullNumber`: Existing PR number
  - `title`: New PR title
  - `body`: New PR description
- Report: "Updated existing PR #X"

#### c. If PR does not exist - Create it
- **CRITICAL:** Use GitHub MCP tool `mcp__github__create_pull_request`
- **NEVER** use `gh pr create` via Bash
- Parameters:
  - `owner`: Repository owner
  - `repo`: "java-sdk"
  - `title`: PR title
  - `head`: Current branch name
  - `base`: Base branch (usually "master")
  - `body`: PR description
- Report: "Created new PR #X"

**Mark Step 6 as completed using TodoWrite before proceeding**

---

### 7. Report Results
**Mark this step as in_progress using TodoWrite**
- Display PR URL
- Show PR title and base branch
- Confirm PR was created successfully

**Mark Step 7 as completed using TodoWrite - PR creation complete!**

---

## Example Usage

User: "/create-pr-current"

Assistant executes:
1. Gets current branch: `jae/add-feature`
2. Checks for merge conflicts with master (if any, resolves interactively)
3. Gets repository information
4. Pushes: `git push -u origin jae/add-feature`
5. Generates PR title and description
6. Creates PR using `mcp__github__create_pull_request`
7. Returns PR URL

**Note:** Triggers are disabled to avoid confusion with create-prs agent.
Must be explicitly invoked with `/create-pr-current` command.

## Error Handling

- If no git repository: Report error
- If on master/main branch: Warn and ask for confirmation
- If uncommitted changes: Offer to show status and ask to commit first
- If push fails: Report error and abort
- If GitHub MCP fails: Report error (do NOT fall back to gh CLI)
