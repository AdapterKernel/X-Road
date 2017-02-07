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

# Test case for verifying that the operational monitoring related data of
# metaservice requests are stored by the operational monitoring daemon.
# It is also verified that central monitoring client has full access to
# operational monitoring data.

import os
import sys

sys.path.append('..')
import python_common as common

def _expected_keys_and_values_of_listmethods_query_rec(
        xroad_message_id, security_server_address, security_server_type):
    return [
        ("clientMemberClass", "GOV"),
        ("clientMemberCode", "00000001"),
        ("clientSecurityServerAddress", "xtee9.ci.kit"),
        ("clientSubsystemCode", "System1"),
        ("clientXRoadInstance", "XTEE-CI-XM"),
        ("messageId", xroad_message_id),
        ("messageProtocolVersion", "4.0"),
        ("requestAttachmentCount", 0),
        ("requestSoapSize", 1117),
        ("responseAttachmentCount", 0),
        ("responseSoapSize", 2780),
        ("securityServerInternalIp", security_server_address),
        ("securityServerType", security_server_type),
        ("serviceCode", "listMethods"),
        ("serviceMemberClass", "GOV"),
        ("serviceMemberCode", "00000000"),
        ("serviceSecurityServerAddress", "xtee8.ci.kit"),
        ("serviceSubsystemCode", "Center"),
        ("serviceVersion", "v1"),
        ("serviceXRoadInstance", "XTEE-CI-XM"),
        ("succeeded", True),
    ]

def _expected_keys_and_values_of_get_ss_metrics_query_rec(
        xroad_message_id, security_server_address, security_server_type):
    return [
        ("clientMemberClass", "GOV"),
        ("clientMemberCode", "00000001"),
        ("clientSecurityServerAddress", "xtee9.ci.kit"),
        ("clientSubsystemCode", "Central monitoring client"),
        ("clientXRoadInstance", "XTEE-CI-XM"),
        ("messageId", xroad_message_id),
        ("messageProtocolVersion", "4.0"),
        ("requestAttachmentCount", 0),
        ("requestSoapSize", 1406),
        ("responseAttachmentCount", 0),
        ("securityServerInternalIp", security_server_address),
        ("securityServerType", security_server_type),
        ("serviceCode", "getSecurityServerMetrics"),
        ("serviceMemberClass", "GOV"),
        ("serviceMemberCode", "00000000"),
        ("serviceSecurityServerAddress", "xtee8.ci.kit"),
        ("serviceXRoadInstance", "XTEE-CI-XM"),
        ("succeeded", True),
    ]

def run(client_security_server_address, producer_security_server_address,
        ssh_user, request_template_dir):
    listmethods_query_template_filename = os.path.join(
            request_template_dir, "listmethods_producer_query_template.xml")
    get_ss_metrics_query_template_filename = os.path.join(
            request_template_dir, "get_ss_metrics_query_template.xml")
    query_data_client_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_central_monitoring_template.xml")
    query_data_producer_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_producer_central_monitoring_template.xml")

    client_timestamp_before_requests = common.get_remote_timestamp(
            client_security_server_address, ssh_user)
    producer_timestamp_before_requests = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)

    message_id_listmethods = common.generate_message_id()
    print("\nGenerated message ID %s for listMethods request" % (message_id_listmethods, ))

    ### Regular and operational data requests and the relevant checks

    print("\n---- Sending a listMethods request to the client's security server ----\n")

    request_contents = common.format_xroad_request_template(
            listmethods_query_template_filename, message_id_listmethods)
    print("Generated the following listMethods request: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part = common.get_multipart_soap(mime_parts[0])
        common.print_multipart_soap(soap_part)

    else:
        common.parse_and_check_soap_response(raw_response)

    message_id_get_ss_metrics = common.generate_message_id()
    print("\nGenerated message ID %s for getSecurityServerMetrics request" % (
        message_id_get_ss_metrics, ))

    print("\n---- Sending a getSecurityServerMetrics request to the client's security server ----\n")

    request_contents = common.format_xroad_request_template(
            get_ss_metrics_query_template_filename, message_id_get_ss_metrics)
    print("Generated the following getSecurityServerMetrics request: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part = common.get_multipart_soap(mime_parts[0])
        # getSecurityServerMetrics response is large, print only headers
        common.print_multipart_soap_headers(soap_part)

    else:
        common.parse_and_check_soap_response(raw_response)

    common.wait_for_operational_data()

    client_timestamp_after_requests = common.get_remote_timestamp(
            client_security_server_address, ssh_user)
    producer_timestamp_after_requests = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)

    # Now make operational data requests to both security servers and check the
    # response payloads.

    print("\n---- Sending an operational data request to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_data_client_template_filename, message_id,
            client_timestamp_before_requests, client_timestamp_after_requests)

    print("Generated the following query data request for the client's security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

        json_payload = common.get_multipart_json_payload(mime_parts[1])

        # Check the presence of all the required fields in at least one JSON structure.
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_listmethods_query_rec(
                    message_id_listmethods, client_security_server_address, "Client"))

        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_get_ss_metrics_query_rec(
                    message_id_get_ss_metrics, client_security_server_address, "Client"))

        # Check if the timestamps in the response are in the expected range.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests, client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    # Central monitoring client is used as a service client in operational 
    # data request. As central monitoring client is registered in client's 
    # security server, let's send the operational data request to producer's 
    # security server via client's security server.
    print("\n---- Sending an operational data request from central monitoring client "\
          "to the producer's security server ----\n")

    message_id = common.generate_message_id()
    print("\nGenerated message ID %s for query data request" % (message_id, ))

    request_contents = common.format_query_operational_data_request_template(
            query_data_producer_template_filename, message_id,
            producer_timestamp_before_requests, producer_timestamp_after_requests)
    print("Generated the following query data request for the producer's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count, is_client=False)

        json_payload = common.get_multipart_json_payload(mime_parts[1])

        # Check the presence of all the required fields in at least one JSON structure.
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_listmethods_query_rec(
                    message_id_listmethods, producer_security_server_address, "Producer"))

        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_get_ss_metrics_query_rec(
                    message_id_get_ss_metrics, producer_security_server_address, "Producer"))

        # Check timestamp values
        common.assert_expected_timestamp_values(
                json_payload,
                producer_timestamp_before_requests, producer_timestamp_after_requests)

        common.assert_equal_timestamp_values(json_payload)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)
