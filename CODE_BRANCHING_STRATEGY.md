# Code Branching Strategy


* BenMAP Cloud developers will create a personal developer branch for their work or, if preferred, use a feature branch for their work on a particularly large feature. Either way, this branch will be created from the default "develop" branch. Personal developer branches can be long-lived. Feature branches may be deleted after merging into develop branch.
    * Example personal developer branch: **develop-janderton**
    * Example feature branch referencing jira ticket: **develop-BCD-999**
* If using a long-lived personal developer branch, the developer will merge the latest change from the "develop" branch into their personal developer branch before beginning work to ensure they are working with the latest code.
* When a developer is ready to submit their work for testing, they will submit a pull request from their developer branch to the default develop branch. This change will be reviewed and merged.
* No work should ever be performed directly in the develop or main branch.




The "develop" branch will automatically build and deploy to the development environment.

The "main" branch will automatically build and deploy to the stage environment.

Only tagged builds from the main branch will be deployed to the production environment.



This process applies to the BenCloudServer and BenCloudApp repositories.