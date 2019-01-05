create view badupdates as (
select
l.session_id as session,
l.federate_id as lfed,
r.federate_id as rfed,
l.instance_handle as instance,
l.attribute_handle as attribute,
l.log_time as time,
l.value as lval,
r.value as rval
from
localupdate as l, remoteupdate as r
where
l.session_id = r.session_id
and
l.federate_id <> r.federate_id
and
l.instance_handle = r.instance_handle
and
l.attribute_handle = r.attribute_handle
and
l.log_time = r.log_time
and
l.value <> r.value);
