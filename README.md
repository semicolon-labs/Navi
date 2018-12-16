<h1 align="center">Navi</h1>
<p> Navi is an Augmented Reality Navigation System aimed at making it safer for people to move and drive around today's busy roads. </p>

<p><b>Prerequisites</b></p>

<ul>
<li>Device should support ARCore. <a href="https://developers.google.com/ar/discover/supported-devices">(See supported devices.)</a></li>
<li>Minimum SDK version 24.</li>
</ul>

<h1>What?</h1>
Navi is an Augmented Reality based navigation system made using 
<ul>
<li><b>Google ARCore</b></li>
<li><b> Google Places API </b></li>
<li><b>Google Directions API</b></li>
</ul>
After calibration and surface detection, Navi renders arrows on the roads that help the drivers or pedestrians navigate through places. A very interesting thing about Navi is that it also helps lost navigators backtrack their route and help them navigate back to where they came from. This means no one will ever get lost again!

<h1>How it works?</h1>
Navi is based off ARCore and it uses ARCore's API to render objects like 3D arrows on the screen. We get the destination details from our users and then the fun begins! Our Prediction API gets predictions of places which the users search  for. After setting the destination, we make requests to Google's Directions API to get directions for navigation from users current location (GPS) to the set destination. The Google's Direction API is called periodically to ensure that the user doesn't deviate from the original course of navigation.
