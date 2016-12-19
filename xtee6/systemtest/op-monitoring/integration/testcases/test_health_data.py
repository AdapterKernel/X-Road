#!/usr/bin/env python3

# The MIT License
# Copyright (c) 2016 Estonian Information System Authority (RIA), Population Register Centre (VRK)
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# Test case for verifying that the health data gathered by the operational
# monitoring daemon can be retrieved and is correct, within the configured
# statistics period.

import os
import sys
import time
import requests

sys.path.append('..')
import python_common as common

# This value is ensured to exist in the configuration of the operational monitoring daemon
# via run_tests.py.
STATISTICS_PERIOD_SECONDS = 10

GET_RANDOM_SERVICE_XML = \
"""<om:service id:objectType="SERVICE"><id:xRoadInstance>XTEE-CI-XM</id:xRoadInstance><id:memberClass>GOV</id:memberClass><id:memberCode>00000000</id:memberCode><id:subsystemCode>Center</id:subsystemCode><id:serviceCode>xroadGetRandom</id:serviceCode><id:serviceVersion>v1</id:serviceVersion></om:service>"""

GET_HEALTH_DATA_SERVICE_XML = \
"""<om:service id:objectType="SERVICE"><id:xRoadInstance>XTEE-CI-XM</id:xRoadInstance><id:memberClass>GOV</id:memberClass><id:memberCode>00000000</id:memberCode><id:serviceCode>getSecurityServerHealthData</id:serviceCode></om:service>"""

LISTMETHODS_SERVICE_XML = \
"""<om:service id:objectType="SERVICE"><id:xRoadInstance>XTEE-CI-XM</id:xRoadInstance><id:memberClass>GOV</id:memberClass><id:memberCode>00000001</id:memberCode><id:subsystemCode>System1</id:subsystemCode><id:serviceCode>listMethods</id:serviceCode><id:serviceVersion>v1</id:serviceVersion></om:service>"""

# For statistical values related to request duration we expect that the fields are
# present but we cannot expect much about the values -- these depend on network load and
# capabilities as well as the load of the target hosts.
# We can expect with some certainty that the SOAP size related data is stable for each
# given service at a given type of server.
# NOTE: health data are gathered only for the requests that have been served in
# the producer (server proxy) role.
SAMPLE_PRODUCER_GET_RANDOM_STATS = {
    "om:successfulRequestCount": 1,
    "om:unsuccessfulRequestCount": 0,
    "om:requestMinDuration": 74.8,
    "om:requestAverageDuration": 78.8,
    "om:requestMaxDuration": 82,
    "om:requestDurationStdDev": 5.65685,
    "om:requestMinSoapSize": 1629,
    "om:requestAverageSoapSize": 1629.0,
    "om:requestMaxSoapSize": 1629,
    "om:requestSoapSizeStdDev": 0.0,
    "om:responseMinSoapSize": 1519,
    "om:responseAverageSoapSize": 1519.0,
    "om:responseMaxSoapSize": 1519,
    "om:responseSoapSizeStdDev": 0.0,
}

SAMPLE_CLIENT_LISTMETHODS_STATS = {
    "om:successfulRequestCount": 1,
    "om:unsuccessfulRequestCount": 0,
    "om:requestMinDuration": 110,
    "om:requestAverageDuration": 110.0,
    "om:requestMaxDuration": 110,
    "om:requestDurationStdDev": 0.0,
    "om:requestMinSoapSize": 1118,
    "om:requestAverageSoapSize": 1118.0,
    "om:requestMaxSoapSize": 1118,
    "om:requestSoapSizeStdDev": 0.0,
    "om:responseMinSoapSize": 2305,
    "om:responseAverageSoapSize": 2305.0,
    "om:responseMaxSoapSize": 2305,
    "om:responseSoapSizeStdDev": 0.0,
}

PREDICTABLE_FIELDS = (
    "om:requestMinSoapSize",
    "om:requestMaxSoapSize",
    "om:requestAverageSoapSize",
    "om:requestSoapSizeStdDev",
    "om:responseMinSoapSize",
    "om:responseAverageSoapSize",
    "om:responseMaxSoapSize",
    "om:responseSoapSizeStdDev",
)

# All these fields must be missing in the lastPeriodStatistics element for a given service if
# the value of om:successfulRequestCount is 0.
STATISTICS_FIELDS = (
    "om:requestMinDuration",
    "om:requestAverageDuration",
    "om:requestMaxDuration",
    "om:requestDurationStdDev",
    "om:requestMinSoapSize",
    "om:requestAverageSoapSize",
    "om:requestMaxSoapSize",
    "om:requestSoapSizeStdDev",
    "om:responseMinSoapSize",
    "om:responseAverageSoapSize",
    "om:responseMaxSoapSize",
    "om:responseSoapSizeStdDev",
)

def run(client_security_server_address, producer_security_server_address,
        ssh_user, request_template_dir):
    xroad_request_template_filename = os.path.join(
            request_template_dir, "simple_xroad_query_template.xml")
    listmethods_query_template_filename = os.path.join(
            request_template_dir, "listmethods_client_query_template.xml")
    soap_fault_query_template_filename = os.path.join(
            request_template_dir, "soap_fault_query_template.xml")

    query_data_client_template_filename = os.path.join(
            request_template_dir, "query_health_data_client_template.xml")
    query_data_producer_template_filename = os.path.join(
            request_template_dir, "query_health_data_producer_template.xml")
    query_data_invalid_client_template_filename = os.path.join(
            request_template_dir, "query_health_data_invalid_client_template.xml")
    query_data_unknown_client_template_filename = os.path.join(
            request_template_dir, "query_health_data_unknown_client_template.xml")
    query_data_without_client_template_filename = os.path.join(
            request_template_dir, "query_health_data_without_client.xml")

    producer_health_data_request = None
    client_health_data_request = None

    producer_initial_timestamp = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)
    producer_opmonitor_restart_timestamp = common.get_opmonitor_restart_timestamp(
            producer_security_server_address, ssh_user)
    client_opmonitor_restart_timestamp = common.get_opmonitor_restart_timestamp(
            client_security_server_address, ssh_user)

    ### First, send a regular X-Road request.

    xroad_message_id = common.generate_message_id()
    print("\nGenerated message ID %s for X-Road request" % (xroad_message_id, ))

    print("\n---- Sending an X-Road request to the client's security server ----\n")

    request_contents = common.format_xroad_request_template(
            xroad_request_template_filename, xroad_message_id)
    print("Generated the following X-Road request: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents)
    print("Received the following X-Road response: \n")

    xml = common.parse_and_clean_xml(response.text)
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    common.wait_for_operational_data()

    ### Make a health check request to the producer.

    print("\n---- Sending a health data request to the producer's " \
            "security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    request_contents = common.format_query_health_data_request_template(
            query_data_producer_template_filename, message_id)
    producer_health_data_request = request_contents

    print("Generated the following health data request for the producer's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            producer_security_server_address, request_contents)

    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    _assert_monitoring_daemon_start_timestamp_in_range(
            response, producer_opmonitor_restart_timestamp)
    _assert_stats_period(response, STATISTICS_PERIOD_SECONDS)

    print("Looking for the xroadGetRandom service in the response")

    event_data = _find_health_data_events_for_service(response, GET_RANDOM_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about xroadGetRandom was not found in the response")

    _assert_last_successful_event_timestamp_in_range(event_data, producer_initial_timestamp)
    _assert_successful_events_count(event_data, 1)
    _assert_unsuccessful_events_count(event_data, 0)

    _assert_xml_tags_present(
            event_data, SAMPLE_PRODUCER_GET_RANDOM_STATS.keys())

    _assert_xml_tags_match_values(
            event_data, PREDICTABLE_FIELDS, SAMPLE_PRODUCER_GET_RANDOM_STATS)

    ### Send a listMethods request to the client.

    listmethods_message_id = common.generate_message_id()
    print("\nGenerated message ID %s for the listMethods request to the client" % (
        listmethods_message_id, ))

    print("\n---- Sending a listMethods request to the client's security server ----\n")

    request_contents = common.format_xroad_request_template(
        listmethods_query_template_filename, listmethods_message_id)
    print("Generated the following X-Road request: \n")
    print(request_contents)

    response = common.post_xml_request(
        client_security_server_address, request_contents, get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part = common.get_multipart_soap(mime_parts[0])
        common.print_multipart_soap(soap_part)

    else:
        common.parse_and_check_soap_response(raw_response)

    common.wait_for_operational_data()

    ### Send a health data request to the client.

    client_pre_health_data_timestamp = common.get_remote_timestamp(
            client_security_server_address, ssh_user)

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    print("\n---- Sending a health data request to the client's " \
            "security server ----\n")

    request_contents = common.format_query_health_data_request_template(
            query_data_client_template_filename, message_id)
    client_health_data_request = request_contents

    print("Generated the following health data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents)

    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    _assert_monitoring_daemon_start_timestamp_in_range(
            response, client_opmonitor_restart_timestamp)
    _assert_stats_period(response, STATISTICS_PERIOD_SECONDS)

    print("Looking for the listMethods service in the response")

    event_data = _find_health_data_events_for_service(response, LISTMETHODS_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about listMethods was not found in the response")

    _assert_last_successful_event_timestamp_in_range(
            event_data, client_pre_health_data_timestamp)
    _assert_successful_events_count(event_data, 1)
    _assert_unsuccessful_events_count(event_data, 0)

    _assert_xml_tags_present(event_data, SAMPLE_CLIENT_LISTMETHODS_STATS.keys())

    _assert_xml_tags_match_values(
            event_data, PREDICTABLE_FIELDS, SAMPLE_CLIENT_LISTMETHODS_STATS)

    ### Send a health data request to the client, using an invalid client ID in the
    ### query criteria.

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    print("\n---- Sending a health data request to the client's " \
            "security server, using an invalid client in the filter criteria ----\n")

    request_contents = common.format_query_health_data_request_template(
            query_data_invalid_client_template_filename, message_id)

    print("Generated the following health data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents)

    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    # Using an invalid client ID must result in a SOAP fault.
    common.assert_soap_fault(xml)

    ### Send an unfiltered health data request to the client, using the producer
    ### as the service provider.

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    print("\n---- Sending an unfiltered health data request to the client's " \
            "security server, using the producer as the service provider ----\n")

    request_contents = common.format_query_health_data_request_template(
            query_data_without_client_template_filename, message_id)

    print("Generated the following health data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(client_security_server_address, request_contents)

    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    # This response must contain several serviceEvents elements (for all the
    # requests that were made to the producer above, including the initial health
    # data request).
    _assert_service_events_min_count(response, 2)

    event_data = _find_health_data_events_for_service(response, GET_RANDOM_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about xroadGetRandom was not found in the response")

    event_data = _find_health_data_events_for_service(response, GET_HEALTH_DATA_SERVICE_XML)
    if event_data is None:
        raise Exception(
                "Health data about getSecurityServerHealthData was not found in the response")

    ### Send a request using an unknown client ID in the filter. Expect an empty response
    ### is returned.

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    print("\n---- Sending a health data request with an unknown client ID to the client's " \
            "security server ----\n")

    request_contents = common.format_query_health_data_request_template(
            query_data_unknown_client_template_filename, message_id)

    print("Generated the following health data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(client_security_server_address, request_contents)
    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    common.check_soap_fault(xml)
    _assert_no_events(response)

    ### Sleep and expect that the health data will be reset.

    print("Waiting for the health metrics to be reset\n")
    time.sleep(STATISTICS_PERIOD_SECONDS)

    ### Repeat the health data requests and check if the health data has been reset.

    print("Repeating the health data request to the producer\n")
    response = common.post_xml_request(
            producer_security_server_address, producer_health_data_request)

    xml = common.parse_and_clean_xml(response.text)
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    _assert_monitoring_daemon_start_timestamp_in_range(
            response, producer_opmonitor_restart_timestamp)
    _assert_stats_period(response, STATISTICS_PERIOD_SECONDS)

    event_data = _find_health_data_events_for_service(response, GET_RANDOM_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about xroadGetRandom was not found in the response")

    _assert_successful_events_count(event_data, 0)
    _assert_unsuccessful_events_count(event_data, 0)
    _assert_xml_tags_missing(event_data, STATISTICS_FIELDS)

    print("Repeating the health data request to the client\n")
    response = common.post_xml_request(
            client_security_server_address, client_health_data_request)

    xml = common.parse_and_clean_xml(response.text)
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    _assert_monitoring_daemon_start_timestamp_in_range(
            response, client_opmonitor_restart_timestamp)
    _assert_stats_period(response, STATISTICS_PERIOD_SECONDS)

    event_data = _find_health_data_events_for_service(response, LISTMETHODS_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about listMethods was not found in the response")

    _assert_successful_events_count(event_data, 0)
    _assert_unsuccessful_events_count(event_data, 0)
    _assert_xml_tags_missing(event_data, STATISTICS_FIELDS)

    ### Now make an unusuccessful request and check the relevant health data.

    producer_pre_unsuccessful_timestamp = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)

    message_id = common.generate_message_id()
    print("\nGenerated message ID %s for an X-Road request that will cause a SOAP fault" % (
        message_id, ))

    print("\n---- Sending an X-Road request that will cause a SOAP fault at the " \
            "service provider, to the client's security server ----\n")

    request_contents = common.format_xroad_request_template(
            soap_fault_query_template_filename, message_id)
    print("Generated the following X-Road request: \n")
    print(request_contents)
 
    response = common.post_xml_request(client_security_server_address, request_contents)

    print("\nReceived the following X-Road response: \n")
    xml = common.parse_and_clean_xml(response.text)
    print(xml.toprettyxml())

    common.assert_soap_fault(xml)

    common.wait_for_operational_data()

    # Send a health check request to the producer.

    print("\n---- Sending a health data request to the producer's " \
            "security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for health data request" % (message_id, ))

    request_contents = common.format_query_health_data_request_template(
            query_data_producer_template_filename, message_id)

    print("Generated the following health data request for the producer's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            producer_security_server_address, request_contents)

    xml = common.parse_and_clean_xml(response.text)
    print("Received the following health data response:\n")
    print(xml.toprettyxml())

    common.check_soap_fault(xml)

    # The service is xRoadGetRandom but the result was a fault.
    print("Looking for the xroadGetRandom service in the response")

    event_data = _find_health_data_events_for_service(response, GET_RANDOM_SERVICE_XML)
    if event_data is None:
        raise Exception("Health data about xroadGetRandom was not found in the response")

    _assert_successful_events_count(event_data, 0)
    _assert_unsuccessful_events_count(event_data, 1)
    _assert_last_unsuccessful_event_timestamp_in_range(
            event_data, producer_pre_unsuccessful_timestamp)

### Helpers

def _parse_xml_for_health_data(health_data_response: requests.Response):
    xml = common.parse_and_clean_xml(health_data_response.text)
    return xml.documentElement.getElementsByTagName(
            "om:getSecurityServerHealthDataResponse")[0]

def _find_all_health_data_events(health_data_response: requests.Response):
    health_data = _parse_xml_for_health_data(health_data_response)
    return health_data.getElementsByTagName("om:serviceEvents")

def _find_health_data_events_for_service(
        health_data_response: requests.Response, service_id: str):
    """ Return the XML subtree of the event data matching the service ID.

    @param health_data_response: a requests.Response object as received from the server
    @param service_id: The XML snippet describing the expected service ID as as string.

    Sample snippet of the health data response:
    <om:getSecurityServerHealthDataResponse>
      <om:monitoringStartupTimestamp>1479127077649</om:monitoringStartupTimestamp>
      <om:statisticsPeriodSeconds>60</om:statisticsPeriodSeconds>
      <om:servicesEvents>
        <om:serviceEvents>
          <om:service id:objectType="SERVICE">
            <id:xRoadInstance>XTEE-CI-XM</id:xRoadInstance>
            <id:memberClass>GOV</id:memberClass>
            <id:memberCode>00000000</id:memberCode>
            <id:subsystemCode>Center</id:subsystemCode>
            <id:serviceCode>xroadGetRandom</id:serviceCode>
            <id:serviceVersion>v1</id:serviceVersion>
          </om:service>
          <om:lastSuccessfulRequestTimestamp>1479127575144</om:lastSuccessfulRequestTimestamp>
          <om:lastPeriodStatistics>
            <om:successfulRequestCount>1</om:successfulRequestCount>
            <om:unsuccessfulRequestCount>0</om:unsuccessfulRequestCount>
            <om:requestMinDuration>78</om:requestMinDuration>
            <om:requestAverageDuration>78.0</om:requestAverageDuration>
            <om:requestMaxDuration>78</om:requestMaxDuration>
            <om:requestDurationStdDev>0.0</om:requestDurationStdDev>
            <om:requestMinSoapSize>1629</om:requestMinSoapSize>
            <om:requestAverageSoapSize>1629.0</om:requestAverageSoapSize>
            <om:requestMaxSoapSize>1629</om:requestMaxSoapSize>
            <om:requestSoapSizeStdDev>0.0</om:requestSoapSizeStdDev>
            <om:responseMinSoapSize>1519</om:responseMinSoapSize>
            <om:responseAverageSoapSize>1519.0</om:responseAverageSoapSize>
            <om:responseMaxSoapSize>1519</om:responseMaxSoapSize>
            <om:responseSoapSizeStdDev>0.0</om:responseSoapSizeStdDev>
          </om:lastPeriodStatistics>
        </om:serviceEvents>
        <om:serviceEvents>
                ...
    """
    health_data = _parse_xml_for_health_data(health_data_response)
    for event_data in health_data.getElementsByTagName("om:serviceEvents"):
        if event_data.getElementsByTagName("om:service"):
            service = event_data.getElementsByTagName("om:service")[0]
            # Return the whole surrounding serviceEvents element.
            if service.toxml() == service_id:
                return event_data

    return None

def _assert_no_events(health_data_response: requests.Response):
    health_data = _parse_xml_for_health_data(health_data_response)
    events = health_data.getElementsByTagName("om:servicesEvents")
    if events[0].childNodes:
        raise Exception("Expecting an empty servicesEvents element")

def _assert_monitoring_daemon_start_timestamp_in_range(
        health_data_response: requests.Response, opmonitor_restart_timestamp: int):
    health_data = _parse_xml_for_health_data(health_data_response)
    # The value of om:monitoringStartupTimestamp is in milliseconds but we query the
    # op-monitor startup timestamp in seconds (for consistency with the rest of the
    # test code).
    startup_timestamp = int(int(health_data.getElementsByTagName(
        "om:monitoringStartupTimestamp")[0].firstChild.nodeValue) / 1000)
    if abs(startup_timestamp - opmonitor_restart_timestamp) > 10:
        # Allow the service startup time and the integer part of the point in time
        # the timestamp is queried in Java code, to differ by some seconds.
        # Under normal circumstances the value seems to differ by around 2 - 4 secs.
        print("Reported monitoring startup time in seconds:", startup_timestamp)
        print("Startup time of xroad-opmonitor:", opmonitor_restart_timestamp)
        raise Exception(
                "Expecting the reported monitoring startup timestamp to be not more than " \
                        "10 seconds later than the service startup timestamp ")

def _assert_stats_period(health_data_response: requests.Response, value: int):
    health_data = _parse_xml_for_health_data(health_data_response)
    health_data_stats_period = int(
            health_data.getElementsByTagName("om:statisticsPeriodSeconds")[0].firstChild.nodeValue)
    if health_data_stats_period != value:
        raise Exception(
                "The monitoring statistics period %d did not match the expected value %d" % (
                    health_data_stats_period, value))

def _assert_service_events_min_count(health_data_response: requests.Response, count: int):
    service_events = _find_all_health_data_events(health_data_response)
    if len(service_events) < 2:
        raise Exception("Expected at least %d serviceEvents elements in the " \
                "health data response" % (count, ))

def _assert_xml_tags_present(event_data_xml_tree, tags):
    for tag in tags:
        if not event_data_xml_tree.getElementsByTagName(tag):
            raise Exception("The '%s' tag is missing in the given XML" % (tag, ))

def _assert_xml_tags_missing(event_data_xml_tree, tags):
    for tag in tags:
        if event_data_xml_tree.getElementsByTagName(tag):
            raise Exception("The '%s' tag is present in the given XML" % (tag, ))

def _assert_xml_tags_match_values(
        event_data_xml_tree, field_names, expected_fields_and_values):
    for field_name in field_names:
        tag_value = event_data_xml_tree.getElementsByTagName(field_name)[0]\
                .firstChild.nodeValue
        if tag_value != str(expected_fields_and_values.get(field_name)):
            raise Exception(
                "The expected value of tag %s (%s) did not match the actual value %s" % (
                    field_name, expected_fields_and_values.get(field_name), tag_value))

def _assert_successful_events_count(event_data_xml_tree, value):
    _assert_events_count(event_data_xml_tree, value, successful=True)

def _assert_unsuccessful_events_count(event_data_xml_tree, value):
    _assert_events_count(event_data_xml_tree, value, successful=False)

def _assert_events_count(event_data_xml_tree, value, successful=True):
    tag = "om:successfulRequestCount" if successful else "om:unsuccessfulRequestCount"
    msg_part = "successful" if successful else "unsuccessful"

    events_count = int(event_data_xml_tree.getElementsByTagName(tag)[0].firstChild.nodeValue)
    if events_count != value:
        raise Exception(
                "The %s events count %d did not match the expected value %d" % (
                    msg_part, events_count, value, ))

def _assert_last_successful_event_timestamp_in_range(event_data_xml_tree, timestamp):
    _assert_last_event_timestamp_in_range(
        event_data_xml_tree, timestamp, successful=True)

def _assert_last_unsuccessful_event_timestamp_in_range(event_data_xml_tree, timestamp):
     _assert_last_event_timestamp_in_range(event_data_xml_tree, timestamp, successful=False)

def _assert_last_event_timestamp_in_range(
        event_data_xml_tree, timestamp, successful=True):
    tag = "om:lastSuccessfulRequestTimestamp" if successful else "om:lastUnsuccessfulRequestTimestamp"
    msg_part = "successful" if successful else "unsuccessful"
    timestamp_ms = timestamp * 1000

    event_timestamp = int(
            event_data_xml_tree.getElementsByTagName(tag)[0].firstChild.nodeValue)
    # Allow at most 5 seconds in the past or future (small clock differences etc).
    lower_limit = timestamp_ms - 5000
    upper_limit = timestamp_ms + 5000
    if not (lower_limit <= event_timestamp <= upper_limit):
        raise Exception(
                "Expecting the last %s event timestamp to be in the range (%d - %d)" % (
                    msg_part, lower_limit, upper_limit, ))
