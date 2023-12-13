# BenMAP Cloud - Release Notes

## CURRENT SNAPSHOT
> [API v0.4.2 and DB v21](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release) | [UI v0.4.2](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release)

### New Features and Improvements
* Improved View Results interaface behavior to reduce unnecessary tab reloading. (BCD-316)
* Improve export so that it's possible to include both health impact and valuation results for a single scenario.  (BCD-299)
* Include Qualifier as a default column in health impact and valuation results. (BCD-321)

### Bug Fixes
* Changed join logic in get_hif_results(), get_valuation_results(), and get_exposure_results() functions to properly handle situations where the same functions is run multiple times. Previously, results were being combined. (BCD-330)
* Change "Endpoint" column label in valuation results to "Health Effect" for consistency. (BCD-331)

### Known Issues
* N/A

## Production 2023-12-13
> [API v0.4.2 and DB v21](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release) | [UI v0.4.2](https://github.com/BenMAPCE/BenCloudServer/tree/r0.4.2-release)

### New Features and Improvements
* Improved View Results interaface behavior to reduce unnecessary tab reloading. (BCD-316)
* Improve export so that it's possible to include both health impact and valuation results for a single scenario.  (BCD-299)
* Include Qualifier as a default column in health impact and valuation results. (BCD-321)

### Bug Fixes
* Changed join logic in get_hif_results(), get_valuation_results(), and get_exposure_results() functions to properly handle situations where the same functions is run multiple times. Previously, results were being combined. (BCD-330)
* Change "Endpoint" column label in valuation results to "Health Effect" for consistency. (BCD-331)

### Known Issues
* N/A
