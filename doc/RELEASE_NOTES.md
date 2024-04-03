# BenMAP Cloud - Release Notes
See additional BenMAP Cloud information on [www.epa.gov](https://www.epa.gov/benmap/benmap-cloud).

## Production 2024-??-?? | [API v0.5.0 and DB v25](https://github.com/BenMAPCE/BenCloudServer/tree/develop) | [UI v0.5.0](https://github.com/BenMAPCE/BenCloudApp/tree/develop)

### New Features and Improvements
* Added Incidence and Prevalence dataset review and management to Data Center / Manage Data. (Multiple stories)
* Add option to automatically assign EPA's default valuation functions to the "Value of effects?" page. (Multiple stories)
* Improved user interface on "Value of effects?" page to ensure the user can always see and access the selected valuation functions for each health impact function. (BCD-345)
* Updated user interface to more closely match the epa.gov website. (BCD-146)
* Changed default aggregation scale for valuation on the "Value of effects?" page from Nation to County for more flexibility when exporting valuation results. Also improved messaging regarding aggregation in the analysis results export dialog. (BCD-341)
* Improvements to air quality upload dialog (BCD-359)
* EPA.gov users may now store up to 1000 completed tasks. Other users are limited to 40. (BCD-358)

### Bug Fixes
* The Older Adults (64-99) Exposure analysis group should be 65-99. (BCD-336)
* Ensure that valuation results are still available if valuation function ids change. (BCD-334)
* Replace valuation function dataset with latest version including the updated School Loss Days function. (BCD-337)
* Changed annual metric on existing air quality surfaces to Mean rather than None. This does not affect calculations currently, but was updated for correctness. (BCD-329)

### Pending Items
* Improvements to user session timeout handling (BCD-233, BCD-224)

### Known Issues
* Large result datasets may fail to export successfully if 12km grid is included. As a workaround, please run fewer functions, fewer scenarios, or export results aggregated to county or state. This will be improved in a future release.

## Production 2023-12-12 | [API v0.4.2 and DB v21](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release) | [UI v0.4.2](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release)

### New Features and Improvements
* Improved View Results interface behavior to reduce unnecessary tab reloading. (BCD-316)
* Improve export so that it's possible to include both health impact and valuation results for a single scenario.  (BCD-299)
* Include Qualifier as a default column in health impact and valuation results. (BCD-321)

### Bug Fixes
* Changed join logic in get_hif_results(), get_valuation_results(), and get_exposure_results() functions to properly handle situations where the same functions is run multiple times. Previously, results were being combined. (BCD-330)
* Change "Endpoint" column label in valuation results to "Health Effect" for consistency. (BCD-331)

### Known Issues
* Valuation results generated using v0.4.0 may not be viewable in this release. The current solution is to rerun the task and generate new results. This will be resolved in a future release.
* Large result datasets may fail to export successfully if 12km grid is included. As a workaround, please run fewer functions, fewer scenarios, or export results aggregated to county or state. This will be improved in a future release.

## Production 2023-10-18 | [API v0.4.0 and DB v20](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.0-release) | [UI v0.4.0](https://github.com/BenMAPCE/BenCloudApp/tree/r0.4.0-release)

### New Features and Improvements
* Added support for aggregation of health impact results before estimating valuation.
* Added support for batch tasks that allow the selection of multiple post-policy air quality scenarios and multiple population years.
* Added additional information fields to air quality surface listing.
* Added support for exposure analysis.
* Added updated EPA standard valuation functions with 2% discount rate option.

### Bug Fixes
* n/a

### Known Issues
* n/a

## Production 2022-09-19 | [API v0.3.0 and DB v10](https://github.com/BenMAPCE/BenCloudServer/tree/prod-release-2022-09-19) | [UI v0.01](https://github.com/BenMAPCE/BenCloudApp/tree/prod-release-2022-09-19)

This was the initial production release of the BenMAP cloud version.