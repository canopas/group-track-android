# Contributing
Thank you so much for your interest in contributing! All types of contributions are encouraged and valued. See below for different ways to help, and details about how this project handles them!

Please make sure to read the relevant section before making your contribution! It will make it a lot easier for us maintainers to make the most of it and smooth out the experience for all involved. 💙

The Project Team looks forward to your contributions.

## Quicklinks

* Ask or Say Something 
  * [Request a Feature](#request-a-feature)
  * [Report an Error or Bug](#report-an-error-or-bug)
  * [Request Support](#request-support)
* Make Something 
  * [Project Setup](#project-setup)
  * [Contribute Code](#contribute-code)
* Manage Something
  * [Provide Support on Issues](#provide-support-on-issues)
  * [Label Issues](#label-issues)
  * [Clean Up Issues and PRs](#clean-up-issues-and-prs)
  * [Merge Pull Requests](#merge-pull-requests)


## Request a Feature

If the project doesn't do something you need or want it to do:

* Open an Issue at https://github.com/canopas/group-track-android/issues
* Provide as much context as you can about what you're running into.
* Please try and be clear about why existing features and alternatives would not work for you.

Once it's filed:

* The project team will [label the issue](#label-issues).
* The project team will evaluate the feature request, possibly asking you more questions to understand its purpose and any relevant requirements. If the issue is closed, the team will convey their reasoning and suggest an alternative path forward.
* If the feature request is accepted, it will be marked for implementation with `feature-accepted`, which can then be done either by a core team member or by anyone in the community who wants to [contribute code](#contribute-code).

Note: The team is unlikely to be able to accept every single feature request that is filed. Please understand if they need to say no.


## Report an Error or Bug

If you run into an error or bug with the project:

* Open an Issue at https://github.com/canopas/group-track-android/issues
* Include *reproduction steps* that someone else can follow to recreate the bug or error on their own.
* Please be ready to provide that information if maintainers ask for it.

Once it's filed:

* The project team will [label the issue](#label-issues).
* A team member will try reproducing the issue with your steps. If there are no repro steps or no obvious way to reproduce the issue, the team will ask you for those steps and mark the issue as `needs-repro`. Bugs with the `needs-repro` tag will not be addressed until they are reproduced.
* If the team can reproduce the issue, it will be marked `needs-fix`, as well as possibly other tags (such as `critical`).
* If you or the maintainers don't respond to an issue for 30 days, the [issue will be closed](#clean-up-issues-and-prs). If you want to come back to it, reply (once, please), and we'll reopen the existing issue. Please avoid filing new issues as extensions of one you already made.
* `critical` issues may be left open, depending on perceived immediacy and severity, even past the 30-day deadline.

 ## Request Support

If you have a question about this project, how to use it, or just need clarification about something:

* Open an Issue at https://github.com/canopas/group-track-android/issues
* Provide as much context as you can about what you're running into.
* Please be ready to provide that information if maintainers ask for it.

Once it's filed:

* The project team will [label the issue](#label-issues).
* Someone will try to have a response soon.
* If you or the maintainers don't respond to an issue for 30 days, the [issue will be closed](#clean-up-issues-and-prs). If you want to come back to it, reply (once, please), and we'll reopen the existing issue. Please avoid filing new issues as extensions of one you already made.

## Project Setup

So you wanna contribute some code? That's great! This project uses GitHub Pull Requests to manage contributions, so [read up on how to fork a GitHub project and file a PR](https://guides.github.com/activities/forking) if you've never done it before.

If this seems like a lot or you aren't able to do all this setup, you might also be able to [edit the files directly](https://help.github.com/articles/editing-files-in-another-user-s-repository/) without having to do any of this setup. Yes, [even code](#contribute-code).

Follow the [requirements](https://github.com/canopas/group-track-android?tab=readme-ov-file#requirements) to run the project and you should be ready to go!


## Contribute Code

We like code commits a lot! They're super handy, and they keep the project going and doing the work it needs to do to be useful to others.
To contribute code:

* [Set up the project](#project-setup).
* Make any necessary changes to the source code.
* Write tests that verify that your contribution works as expected.
* Write clear, concise commit message(s).
* Go to https://github.com/canopas/group-track-android/pulls and open a new pull request with your changes.
* If your PR is connected to an open issue, add a line in your PR's description that says `Fixes: #123`, where `#123` is the number of the issue you're fixing.

Once you've filed the PR:

* Barring special circumstances, maintainers will not review PRs until all checks pass.
* One or more maintainers will use GitHub's review feature to review your PR.
* If the maintainer asks for any changes, edit your changes, push, and ask for another review. Additional tags (such as `needs-tests`) will be added depending on the review.
* If the maintainer decides to pass on your PR, they will thank you for the contribution and explain why they won't be accepting the changes. That's ok! We still really appreciate you taking the time to do it, and we don't take that lightly. 💚
* If your PR gets accepted, it will be marked as such and merged into the `master` branch soon after.

  
## Provide Support on Issues

Helping out other users with their questions is an awesome way of contributing to any community. It's not uncommon for most of the issues on open-source projects to be support-related questions by users trying to understand something they ran into or find their way around a known bug.

Sometimes, the `support` label will be added to things that turn out to be other things, like bugs or feature requests. In that case, suss out the details with the person who filed the original issue, add a comment explaining what the bug is, and change the label from `support` to `bug` or `feature`. If you can't do this yourself, @mention a maintainer so they can do it.

To help other folks out with their questions:

* Go to the issue tracker and [filter open issues by the `support` label](https://github.com/canopas/group-track-android/pulls?q=is%3Aopen+is%3Aissue+label%3Asupport).
* Read through the list until you find something that you're familiar enough with to answer.
* Respond to the issue with whatever details are needed to clarify the question, or get more details about what's going on.
* Once the discussion wraps up and things are clarified, either close the issue or ask the original issue filer (or a maintainer) to close it for you.

Some notes on picking up support issues:

* Avoid responding to issues you don't know you can answer accurately.
* As much as possible, try to refer to past issues with accepted answers. Link to them from your replies with the `#123` format.
* Be kind and patient with users -- often, folks who have run into confusing things might be upset or impatient. This is ok. Try to understand where they're coming from, and if you're too uncomfortable with the tone, feel free to stay away or withdraw from the issue.


## Label Issues

One of the most important tasks in handling issues is labelling them usefully and accurately. All other tasks involving issues ultimately rely on the issue being classified so that relevant parties looking to do their tasks can find them quickly and easily.

To label issues, [open up the list of unlabeled issues](https://github.com/canopas/group-track-android/issues?q=is%3Aopen+is%3Aissue+no%3Alabel+) and, **from newest to oldest**, read through each one and apply issue labels according to the table below. If you're unsure about what label to apply, skip the issue and try the next one: don't feel obligated to label every issue yourself!

Label | Apply When | Notes
--- | --- | ---
`bug` | Cases where the code (or documentation) is behaving in a way it wasn't intended to. | If something is happening that surprises the *user* but does not go against the way the code is designed, it should use the `enhancement` label.
`critical` | Added to `bug` issues if the problem described makes the code completely unusable in a common situation. |
`documentation` | Added to issues or pull requests that affect any of the documentation for the project. | Can be combined with other labels, such as `bug` or `enhancement`.
`duplicate` | Added to issues or PRs that refer to the same issue as another one that's been previously labelled. | Duplicate issues should be marked and closed right away, with a message referencing the issue it's a duplicate of (with `#123`)
`enhancement` | Added to [feature requests](#request-a-feature), PRs, or purely additive documentation issues: the code or docs currently work as expected, but a change is being requested or suggested. |
`help wanted` | Generally, this means it's a lower priority for the maintainer team to itself implement, but that the community is encouraged to pick up if they so desire | Never applied on first-pass labelling.
`in-progress` | Applied by Committers to PRs that are pending work before being ready for review. | The original PR submitter should @mention the team member who applied the label once the PR is complete.
`performance` | This issue or PR is directly related to improving performance. |
`refactor` | Added to issues or PRs that deal with cleaning up or modifying the project for the betterment of it. |
`support` | This issue is either asking a question about how to use the project, clarifying the reason for unexpected behaviour, or possibly reporting a `bug` but does not have enough detail yet to determine whether it would count as such. | The label should be switched to `bug` if reliable reproduction steps are provided. Issues primarily with unintended configurations of a user's environment are not considered bugs, even if they cause crashes.
`tests` | This issue or PR either requests or adds primarily tests to the project. | 
`wont fix` | Apply this label to issues that have nothing at all to do with the project or are otherwise entirely outside of its scope/sphere of influence or the maintainer decide to pass on an otherwise relevant issue. | The issue or PR should be closed as soon as the label is applied, and a clear explanation provided of why the label was used. Contributors are free to contest the labelling, but the decision ultimately falls on committers as to whether to accept something or not.

## Clean Up Issues and PRs

Issues and PRs can go stale after a while. Maybe they're abandoned. Maybe the team will just not have time to address them any time soon.

In these cases, they should be closed until they're brought up again or the interaction starts over.

To clean up issues and PRs:

* Search the issue tracker for issues or PRs, and add the term `updated:<=YYYY-MM-DD`, where the date is 30 days before today.
* Go through each issue *from oldest to newest*, and close them if **all of the following are true**:
  * not opened by a maintainer
  * not marked as `critical`
  * not marked as `starter` or `help wanted` (these might stick around for a while, in general, as they're intended to be available)
  * no explicit messages in the comments asking for it to be left open
  * does not belong to a milestone
* Leave a message when closing saying "Cleaning up the stale issue. Please reopen or ping us if and when you're ready to resume this.

Merge Pull Requests 
----------------------------------------------------------
* Only for contributors with write access
* Use ["Squash and Merge"](https://github.com/blog/2141-squash-your-commits) by default for individual contributions unless requested by the PR author.
  Do so, even if the PR contains only one commit. It creates a simpler history than "Create a Merge Commit".
  Reasons that PR authors may request "Merge and Commit" may include (but are not limited to):

  - The change is easier to understand as a series of focused commits.
  - Contributor is using an e-mail address other than the primary GitHub address and wants that preserved in the history. Contributors must be willing to squash
    the commits manually before acceptance. 
