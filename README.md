# auto-fi

Auto Fi automatically connects to nearby open wifi networks, attempts to click through captive
portal agreements and then tunnels all traffic over a secure VPN.

Auto Fi uses [OpenVPN](https://openvpn.net/), based heavily on the
[ics-openvpn Android app](https://github.com/schwabe/ics-openvpn).

## Privacy

Privacy is important, especially for security applications. Auto Fi is completely open source
to provide greater transparency. Auto Fi uses [Firebase](https://firebase.google.com/) crash
reporting and analytics to collect non-identifiable information to make the app better.
All traffic over Auto Fi's VPN is never logged, recorded or collected, it is encrypted
per-device using a uniquely generated encryption key that never leaves the device and transmitted
from the device to the VPN server where it then continue to it's destination over the Internet.

## Disclaimer

This software utilizes encryption and may or may not be legal depending on the country of residence
or usage. The software is provided "AS IS", without warranty of any kind, express or implied, including but
not limited to the warranties of merchantability, fitness for a particular purpose and
noninfringement. In no event shall the authors or copyright holders be liable for any claim, damages
or other liability, whether in an action of contract, tort or otherwise, arising from, out of or in
connection with the software or the use or other dealings in the software.

## License

auto-fi is open source and available under the GPL v2 license. See the
[LICENSE](LICENSE) file for more info.
