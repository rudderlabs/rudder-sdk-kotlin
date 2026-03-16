:crown: **Automated Release PR**

This pull request was created automatically by the release script. It merges the release branch (`{{RELEASE_BRANCH}}`) into the `main` branch.

This ensures that the latest release branch changes are incorporated into the `main` branch for production.

### Details

- **Monorepo Release Version**: v{{MONOREPO_VERSION}}
- **Release Branch**: `{{RELEASE_BRANCH}}`
- **Related Ticket**: [{{TICKET_ID}}](https://linear.app/rudderstack/issue/{{TICKET_ID}})

### Version Updates

| Module | New Version |
|--------|-------------|
{{VERSION_TABLE}}

---
Please review and merge when ready. :rocket:
