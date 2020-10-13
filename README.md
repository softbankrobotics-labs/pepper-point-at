# Pepper Point At Library

This Android Library will help you make Pepper point at a nearby position by running a specific animation or by giving a Frame to point at.


## Getting Started

### Prerequisites

A robotified project for Pepper with QiSDK. Read the [documentation](https://developer.softbankrobotics.com/pepper-qisdk) if needed.

### Running the Sample Application

The project comes complete with a sample project. You can clone the repository, open it in Android Studio, and run this directly onto a Robot.

The sample app provides a screen representing Pepper in the middle of a room with different animal emojis displayed around him.
If you press an emoji or if you say the name of the animal it's representing, Pepper will point at its position.

Full implementation details are available to see in the project.

### Installing

[**Follow these instructions**](https://jitpack.io/#softbankrobotics-labs/pepper-point-at)

Make sure to replace 'Tag' by the number of the version of the library you want to use.


## Usage

*This README assumes some standard setup can be done by the user, such as initialising variables or implementing code in the correct functions. Refer to the Sample Project for full usage code.*

Initialise the QiSDK in the onCreate. If you are unsure how to do this, refer to the [QiSDK tutorials](https://developer.softbankrobotics.com/pepper-qisdk/getting-started/creating-robot-application)
```
QiSDK.register(this, this)
```
In the `onRobotFocusGained`, instantiate a `PointAtAnimator` object by passing it the QiContext. 

```
override fun onRobotFocusGained(qiContext: QiContext) {
    Log.i(TAG, "onRobotFocusGained")
    pointAtAnimator = PointAtAnimator(qiContext)
}
```
You can them launch a specific `PointAtAnimation` with the following code:
```
pointAtAnimator.playPointAnimation(PointAtAnimation.MEDIUM_LEFT)
```
Here is the list of the different `PointAtAnimation`:
```
CLOSE_FRONT_LEFT
CLOSE_FRONT_RIGHT
CLOSE_MEDIUM_LEFT
CLOSE_MEDIUM_RIGHT
CLOSE_HALF_LEFT
CLOSE_HALF_RIGHT
FRONT_LEFT
FRONT_RIGHT
MEDIUM_LEFT
MEDIUM_RIGHT
HALF_LEFT
HALF_RIGHT
```
You can also ask Pepper to point at a specific `Frame`:
```
pointAtAnimator.pointAt(targetFrame)
```
Refer to this [documentation](https://developer.softbankrobotics.com/pepper-qisdk/api/motion/reference/frame)
if you are not familiar with the concept of `Frame`


## Additional information

When asking the library to point at a specific `Frame`, the library will define if it is close to Pepper or not to run the right `PointAtAnimation`. A "close" `Frame` is closer than 3 meters from Pepper.

For instance, if the library is asked to point at a `Frame` that is one meter directly to the left of Pepper, the `PointAtAnimation` CLOSE_HALF_LEFT will be played. 
But if the library is asked to point at a `Frame` that is ten meters away directly to the left of Pepper, the `PointAtAnimation` HALF_LEFT will be played.


## License

This project is licensed under the BSD 3-Clause "New" or "Revised" License- see the [LICENSE](LICENSE.md) file for details.