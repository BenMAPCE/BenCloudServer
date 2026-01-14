# BenMAP Cloud - Release Notes
See additional BenMAP Cloud information on [www.epa.gov](https://www.epa.gov/benmap/benmap-cloud).


## Production 2026-01-?? (in progress)  | [API v1.1.0 and DB 92](https://github.com/BenMAPCE/BenCloudServer/tree/develop) | [UI v1.1.0](https://github.com/BenMAPCE/BenCloudApp/tree/develop)

### New Features and Improvements 

* Air quality uploads can now receive multiple csv files in one submission. These uploaded files will then be processed in the background. Progress can be monitored in the task manager. (BWD-14, BWD-186, BWD-249)
* Analysis results will now display more quickly when viewing them in the user interface. (BWD-141)
* The EPA HERO ID and Study Url are now available to help users better understand BenMAP's health impact functions. (BWD-169)
* Users may now upload their own health impact and valuation functions for use during analysis. (BWD-171)
* We increased the memory for certain database queries and started tracking task IDs in database sessions to prevent result duplication caused by unresponsive database. (BWD-168, BWD-213) 
* When selecting air quality data, users can now filter by the group names using the newly added dropdown menu. (BWD-253) 
* For health effect analysis, users can now narrow down the study area by selecting a pre-uploaded grid definition at the “Where do you want to perform your analysis?” screen. (BWD-196, BWD-224, BWD-195) 
* The health effects of all default mortality HI and valuation functions are now split into subgroups (“short-term”, “long-term”, and “infant”) based on the Timing field and age ranges. This improvement will help the application automatically select valuation functions. Since their display names are not changed, users may not see any changes from the UI (BWD-206) 

### Dataset Updates

* Replaced age-stratified all race/ethnicity mortality incidence rates for year 2020-2060, health effect Mortality, All Cause and Mortality, Respiratory rates for all states. (BWD-209) 
* Added Hawaii and Alaska data to most datasets. For datasets that do not include Hawaii and Alaska, “Contiguous U.S.” has been added to the dataset name. (e.g. “Contiguous US CMAQ 12km Nation - 2020”. (BWD-145, BWD-176, BWD-183, BWD-184, BWD-187, BWD-188) 
* The following columns and corresponding data have been added to the valuation function template and database: access url, valuation type, multiyear, multiyear dr, and multiyear cost. (BWD-179, BWD-225) 
* In health impact functions, “Seasonal Metric” and “Metric Statistics” are now replaced by column “Timing”. Acceptable values are “Daily” and “Annual”. (BWD-174) 
* Added the default function group “Chronic Effects - Primary” and assigned 21 functions to this group (BWD-205). 
* Katsouyanni’s mortality HIFs are removed from “Premature Death – Primary” and added to “Premature Death – All” group. Their HERO IDs and reference URLs are also updated. (BWD-177) 
* Added Dementia health impact function and valuation function to groups “Chronic Effects - Primary” and “Chronic Effects - All”. Also added state-level dementia incidence data to the newly created incidence dataset with name “State”. (BWD-223, BWD-242)  
* To prevent population loss during area-weighted cross-walking, the default CMAQ 12km Nation grid definition has been replaced with a version clipped to the shoreline to exclude cells in the ocean. All default air quality, population, and incidence data now use the updated version. The previous version has been archived in the database. (BWD-234, BWD-235) 
* If existing user-uploaded datasets have the same name as the newly added default datasets, user IDs will be appended to the dataset name.  (BWD-243) 
* Added prototype state air quality surfaces. Renamed and deleted some prototype 12km air quality surfaces. (BWD-241, BWD-256) 

### Cosmetic Changes and Bug Fixes

* Removed unnecessary dollar sign ($) from formatted health results. Added formatted results to valuation results screen. Re-ordered columns in result screen (BWD-180, BWD-181) 
* The result summary screen now shows the grid definition of air quality data under “Geography”. (BWD-178) 
* Changed term “endpoint” to “health_effect” in exported csv, and function import template (BWD-190, BWD-173) 
* Handling of long-running tasks and task cancelation has been improved for performance and stability. (BWD-168)


## Production 2025-07-22  | [API v1.0.0 and DB 62](https://github.com/BenMAPCE/BenCloudServer/tree/develop) | [UI v1.0.0](https://github.com/BenMAPCE/BenCloudApp/tree/develop)

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