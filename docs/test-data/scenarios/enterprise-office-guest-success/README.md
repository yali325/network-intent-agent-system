# enterprise-office-guest-success

This scenario captures the Intent-stage expectation for the main enterprise
office, guest, and server access-control example.

It covers:

- office -> server: ALLOW
- guest -> server: DENY
- office <-> guest: DENY through an ISOLATION relation
- OSPF as a routing protocol preference only

The expected intent data intentionally excludes devices, interfaces, VLANs, IP
addresses, topology, ACL commands, routing configuration, and CLI text. Later
MAC-TAV stages may plan or configure those details, but IntentAgent must only
preserve the business intent and user preference.
