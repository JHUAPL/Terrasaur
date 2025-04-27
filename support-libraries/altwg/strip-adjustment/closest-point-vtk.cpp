#include <stdio.h>
#include <vtkPolyDataReader.h>
#include <vtkPolyDataWriter.h>
#include <vtkOBJReader.h>
#include <vtkPolyData.h>
#include <vtkCellLocator.h>
#include <vtkPointLocator.h>
#include <vtkGenericCell.h>
#include <vtkMath.h>
#include "lidardata.h"
#include "util.h"

static bool usePointLocator;

static vtkPointLocator* pointLocator = 0;
static vtkCellLocator* cellLocator = 0;
static vtkGenericCell* genericCell = 0;

static vtkPolyData* polyData = 0;

void initializeVtk(const char* dskfile)
{
	polyData = vtkPolyData::New();
	if (endsWith(dskfile, ".vtk"))
	{
		vtkPolyDataReader* smallBodyReader = vtkPolyDataReader::New();
		smallBodyReader->SetFileName(dskfile);
		smallBodyReader->Update();

		polyData->ShallowCopy(smallBodyReader->GetOutput());
		usePointLocator = true;
	}
	else
	{
		vtkOBJReader* smallBodyReader = vtkOBJReader::New();
		smallBodyReader->SetFileName(dskfile);
		smallBodyReader->Update();

		polyData->ShallowCopy(smallBodyReader->GetOutput());
		usePointLocator = false;
	}

	if (usePointLocator)
	{
		// Initialize the point locator
		pointLocator = vtkPointLocator::New();
		pointLocator->FreeSearchStructure();
		pointLocator->SetDataSet(polyData);
		pointLocator->AutomaticOn();
		pointLocator->SetMaxLevel(100);
		pointLocator->SetNumberOfPointsPerBucket(1);
		pointLocator->BuildLocator();
	}
	else
	{
		// Initialize the cell locator
		cellLocator = vtkCellLocator::New();
		cellLocator->FreeSearchStructure();
		cellLocator->SetDataSet(polyData);
		cellLocator->CacheCellBoundsOn();
		cellLocator->AutomaticOn();
		cellLocator->SetMaxLevel(100);
		cellLocator->SetNumberOfCellsPerNode(1);
		cellLocator->BuildLocator();
		genericCell = vtkGenericCell::New();
	}

}

void savePointCloudToVTK(const LidarPointCloud& pointCloud, const std::string& filename)
{
	vtkPoints* points = vtkPoints::New();
	for (unsigned int i=0; i<pointCloud.size(); ++i)
	{
		const Point& p = pointCloud[i];
		points->InsertNextPoint(p.targetpos);
	}

	vtkPolyData* polyData = vtkPolyData::New();
	polyData->SetPoints(points);
	vtkPolyDataWriter* writer = vtkPolyDataWriter::New();
	writer->SetInputData(polyData);
	writer->SetFileName(filename.c_str());
	writer->SetFileTypeToBinary();
	writer->Update();
}

void findClosestPointVtk(const double* origin, double* closestPoint, int* found)
{
	double point[3] = {origin[0], origin[1], origin[2]};
	vtkIdType cellId;
	if (usePointLocator)
	{
		vtkIdType vtkId = pointLocator->FindClosestPoint(point);
		polyData->GetPoint(vtkId, closestPoint);
		cellId = 1; // there will always be a closest point
	}
	else
	{
		int subId;
		double dist2;
		cellLocator->FindClosestPoint(point, closestPoint, genericCell, cellId, subId, dist2);
	}

	if (cellId >= 0)
		*found = 1;
	else
		*found = 0;
}
