# Dynamic discovery configuration guide

## Registering party in SML

SMP Servicegroup - participant identifier, participant scheme = AP finalrecipient type, value

SMP - Document idnetifier: scheme::value, the same thing in AP is action only, in case of oasis schema and value are separated by :: and separated by nifty regex,
in case of peppol they are separated by :: and crude parsing for default schema is done. 

SMP ProcessScheme, ProcessIdentifier, in Service, type+value

  In PMODE

        <services>
            <service name="testService1" value="bdx:noprocess" type="srvtype"/>
        </services>

  In Message

        <ns:CollaborationInfo>
            <ns:Service type="srvtype">bdx:noprocess</ns:Service>
            <ns:Action>TC1Leg1</ns:Action>
        </ns:CollaborationInfo>

SMP TransportProfile, magic constant - bdxr-transport-ebms3-as4-v1p0 on AP can be changed using property domibus.dynamicdiscovery.transportprofileas4

