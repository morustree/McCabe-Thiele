[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.16048526.png)](https://doi.org/10.5281/zenodo.16048526)


# McCabe–Thiele Method (Android/Kotlin)

A graphical implementation of the McCabe-Thiele method for binary distillation under constant and uniform pressure. Supports total condenser (saturated‑liquid distillate), partial condenser (single vapor outlet), and partial reboiler operation. Assumes a single feed stage and no sidestreams.


## 📝 Overview

This project is a rewritten and modernized version of an older Scilab implementation originally developed during my engineering studies.
The original Scilab project includes an additional calculation mode: a partial condenser with liquid distillate + vapor distillate.


## 🛠️ Technical Features

### 🔹 Numerical Computation
The app uses the Apache Commons Math library for Polynomial root solving, for curve intersection calculations and for Building Akima splines from equilibrium curve data points.
This ensures smooth, stable interpolation and accurate stage‑by‑stage stepping.

### 🔹 Graphical Plotting
Graph rendering is implemented using MPAndroidChart, providing smooth equilibrium and operating‑line curves, and interactive zoom.


## 🎥 Video
A walkthrough video demonstrating the Android app:

[![Demo Video](https://img.youtube.com/vi/mHruwOj32es/hqdefault.jpg)](https://youtu.be/mHruwOj32es)
