# Terrasaur Changelog

## July 30, 2025 - v1.1.0

- Updates to existing tools
    - AdjustShapeModelToOtherShapeModel
        - fix intersection bug
    - CreateSBMTStructure
        - new options: -flipX, -flipY, -spice, -date, -observer, -target, -cameraFrame
    - ValidateNormals
        - new option: -fast to only check for overhangs if center and normal point in opposite directions

- New tools:
    - FacetInfo: Print info about a facet
    - PointCloudOverlap: Find points in a point cloud which overlap a reference point cloud
    - TriAx: Generate a triaxial ellipsoid in ICQ format

## April 28, 2025 - v1.0.1

- Add MIT license to repository and source code

## April 27, 2025 - v1.0.0

- Initial release
  