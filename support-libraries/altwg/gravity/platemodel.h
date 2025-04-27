#ifndef PLATEMODEL_H
#define PLATEMODEL_H

#include <vector>
#include <string>
#include "mathutil.h"

/**
  This is bare bones plate model class similar to VTK's vtkPolydata class
  for use in programs which do not have a dependency on VTK.
  */
class Platemodel
{
    struct Vertex
    {
        double point[3];
    };

    struct Plate
    {
        int cell[3];
        double normal[3];
    };

public:
    Platemodel();

    void load(std::string filename);

    void getPoint(int i, double x[3])
    {
        const Vertex& pt = vertices[i];
        x[0] = pt.point[0];
        x[1] = pt.point[1];
        x[2] = pt.point[2];
    }

    void getNormal(int i, double x[3])
    {
        const Plate& p = plates[i];
        x[0] = p.normal[0];
        x[1] = p.normal[1];
        x[2] = p.normal[2];
    }

    void getPlatePoints(int i, int ids[3])
    {
        const Plate& p = plates[i];
        ids[0] = p.cell[0];
        ids[1] = p.cell[1];
        ids[2] = p.cell[2];
    }

    void getPlateCenter(int i, double center[3])
    {
        const Plate& p = plates[i];
        TriangleCenter(
                    vertices[p.cell[0]].point,
                    vertices[p.cell[1]].point,
                    vertices[p.cell[2]].point,
                    center
                    );
    }

    double getPlateArea(int i)
    {
        const Plate& p = plates[i];
        return TriangleArea(
                    vertices[p.cell[0]].point,
                    vertices[p.cell[1]].point,
                    vertices[p.cell[2]].point
                    );
    }

    int getNumberOfPoints()
    {
        return vertices.size();
    }

    int getNumberOfPlates()
    {
        return plates.size();
    }

private:

    void loadGaskell(std::string filename);
    void loadOBJ(std::string filename);

    std::vector<Vertex> vertices;
    std::vector<Plate> plates;
};

#endif // PLATEMODEL_H
