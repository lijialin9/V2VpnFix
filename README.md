# V2VpnFix
Fix the issue of  v2vpn 5.6.11 background VPN connections being terminated by the system  for WSA.

Built on Magisk and LSPosed, this module hooks onStateChangedto monitor and record relevant components and settings. When a system cleanup is triggered, the hook catches it and fires a simulated click by calling the click event's entry point.

WSA：https://github.com/MustardChef/WSABuilds
