.. _TriAx:

=====
TriAx
=====

TriAx is an implementation of the SPC tool TRIAX, which generates a triaxial ellipsoid in ICQ format.

*****
Usage
*****

.. include:: ../toolDescriptions/TriAx.txt
    :literal:

*******
Example
*******

Generate an ellipsoid with dimensions 10, 8, 6, with q = 8.

::

    TriAx -A 10 -B 8 -C 6 -Q 8 -output triax.icq -saveOBJ 

The following ellipsoid is generated:

.. container:: figures-row

    .. figure:: images/TriAx_X.png
        :alt: looking down from the +X direction

        looking down from the +X direction

    .. figure:: images/TriAx_Y.png
        :alt: looking down from the +Y direction

        looking down from the +Y direction

    .. figure:: images/TriAx_Z.png
        :alt: looking down from the +Z direction

        looking down from the +Z direction