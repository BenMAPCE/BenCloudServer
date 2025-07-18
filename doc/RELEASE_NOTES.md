# BenMAP Cloud - Release Notes
See additional BenMAP Cloud information on [www.epa.gov](https://www.epa.gov/benmap/benmap-cloud).

## Production 2025-07-??  | [API v1.0.0 and DB 61](https://github.com/BenMAPCE/BenCloudServer/tree/develop) | [UI v1.0.0](https://github.com/BenMAPCE/BenCloudApp/tree/develop)

### New Features and Improvements
* Analysis exports will now be processed as a background task (similar to analysis tasks) and can be downloaded from the task queue when they are complete. This will prevent large exports from failing due to request timeouts and API memory issues.
* Users can now upload shapefiles to create custom grid definitions that can be associated with air quality surfaces and also used for aggregation during export of results. Please note that BenMAP uses crosswalk tables to translate from one grid to another. The grids provided by EPA use population-weighted crosswalks which are considered more accurate in some circumstances. However, please note that BenMAP generates crosswalks for grids  uploaded by users using area-weighted crosswalks. (BWD-10, et al)
* The completed tasks queue has been sorted so recent tasks will now show at the top. Previously, the most recent tasks were found at the bottom of that list requiring a lot of scrolling for users with numerous tasks. (BWD-93)
* The population dataset has been updated based on the 2020 US census and also restructured to reduce space and increase performance.
* When running exposure analysis, BenMAP will now supply values both for the requested population groups as well as for those that are outside of each group. These are referred to as complementary groups. (BWD-6)
* The layout of the data center page has been revised to improve efficiency and allow for future enhancements. (BWD-29)
* The US EPA standard valuation functions will now include results with 2%, 3%, and 7% discount applied where appropriate. (BWD-84)
* With this release, we are beginning to implement mapping functionality in BenMAP. Currently, this is only visible when viewing grid definitions, but more functionality is planned for the next release. (BWD-18)
* EPA banner is now set to scroll with the rest of the page instead of being locked at the top of the page. (BWD-53)
* Completed tasks will now display their time and date of completion in the user's timezone. (BWD-44)
* Result exports are now treated as separate tasks in the task manager. (BWD-56, BWD-57)
* Added warnings for when file size limits are exceeded for dataset uploads, specifically for air quality, grid definitions, and incidence uploads. (BWD-99)
* Allow administrator to add a global notification banner that will show on all BenMAP web pages. (BWD-33, BWD-109, BWD-110, BWD-111)
* If you are inactive for several minutes, BenMAP will display a warning when your login session is about to expire. Once expired, BenMAP presents an option to help the user log back in. (BWD-25)
* Many more small enhancements...

### Bug Fixes
* Large health impact and valuation analyses were crashing in certain scenarios. Memory configuration adjustments have been made to protect against this. (BWD-1)
* Health impact functions are only shown when they match the air quality surface metrics. Previously, some functions were executed even though the required air quality metric was not present. (BWD-5, BWD-8)
* Valuation results were being duplicated when multiple hif groups are selected with overlapping functions (BWD-3)
* When creating crosswalks, cast col/row to BIGINT first to avoid "integer out of range" error. (BWD-106)

## Production 2024-04-29 | [API v0.5.0 and DB v27](https://github.com/BenMAPCE/BenCloudServer/tree/develop) | [UI v0.5.0](https://github.com/BenMAPCE/BenCloudApp/tree/develop)

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

### Known Issues
* Large result datasets may fail to export successfully if 12km grid is included. As a workaround, please run fewer functions, fewer scenarios, or export results aggregated to county or state. This will be improved in a future release. (BCD-361, BCD-362)
* Improvements are needed to improve application behavior when a user's session times out after 15 minutes. Currently, user's must refresh the page in the browser to log in and continue using the application. (BCD-233, BCD-224)

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