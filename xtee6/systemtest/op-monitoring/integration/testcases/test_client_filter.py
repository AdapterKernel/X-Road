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

# Test case for verifying that the security server owner and the central 
# monitoring client have access to all operational monitoring data records, 
# but regular clients only have access to records that are associated with 
# that client. It is also verified that in case a client is specified in the 
# search criteria of operational monitoring data request, only the data records
# where the specified client is in service provider role are returned.

import os
import sys

sys.path.append('..')
import python_common as common

def _expected_keys_and_values_of_simple_query_rec(
        xroad_message_id, security_server_address, security_server_type):
    return [
        ("clientMemberClass", "GOV"),
        ("clientMemberCode", "00000001"),
        ("clientSecurityServerAddress", "xtee9.ci.kit"),
        ("clientSubsystemCode", "System1"),
        ("clientXRoadInstance", "XTEE-CI-XM"),
        ("messageId", xroad_message_id),
        ("messageIssue", "453465"),
        ("messageProtocolVersion", "4.0"),
        ("messageUserId", "EE37702211230"),
        ("representedPartyClass", "COM"),
        ("representedPartyCode", "UNKNOWN_MEMBER"),
        ("requestAttachmentCount", 0),
        ("requestSoapSize", 1629),
        ("responseAttachmentCount", 0),
        ("responseSoapSize", 1519),
        ("securityServerInternalIp", security_server_address),
        ("securityServerType", security_server_type),
        ("serviceCode", "xroadGetRandom"),
        ("serviceMemberClass", "GOV"),
        ("serviceMemberCode", "00000000"),
        ("serviceSecurityServerAddress", "xtee8.ci.kit"),
        ("serviceSubsystemCode", "Center"),
        ("serviceVersion", "v1"),
        ("serviceXRoadInstance", "XTEE-CI-XM"),
        ("succeeded", True),
    ]

def _expected_keys_and_values_of_query_data_client_proxy_rec(
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
        ("requestSoapSize", 1669),
        ("responseAttachmentCount", 1),
        ("responseMimeSize", 1791),
        ("responseSoapSize", 1757),
        ("securityServerInternalIp", security_server_address),
        ("securityServerType", security_server_type),
        ("serviceCode", "getSecurityServerOperationalData"),
        ("serviceMemberClass", "GOV"),
        ("serviceMemberCode", "00000000"),
        ("serviceSecurityServerAddress", "xtee8.ci.kit"),
        ("serviceXRoadInstance", "XTEE-CI-XM"),
        ("succeeded", True),
    ]

def _expected_keys_and_values_of_query_data_server_proxy_rec(
        xroad_message_id, security_server_address, security_server_type):
    return [
        ("clientMemberClass", "GOV"),
        ("clientMemberCode", "00000000"),
        ("clientSecurityServerAddress", "xtee8.ci.kit"),
        ("clientXRoadInstance", "XTEE-CI-XM"),
        ("messageId", xroad_message_id),
        ("messageProtocolVersion", "4.0"),
        ("requestAttachmentCount", 0),
        ("requestSoapSize", 1610),
        ("responseAttachmentCount", 1),
        ("responseMimeSize", 1732),
        ("responseSoapSize", 1698),
        ("securityServerInternalIp", security_server_address),
        ("securityServerType", security_server_type),
        ("serviceCode", "getSecurityServerOperationalData"),
        ("serviceMemberClass", "GOV"),
        ("serviceMemberCode", "00000001"),
        ("serviceSecurityServerAddress", "xtee9.ci.kit"),
        ("serviceXRoadInstance", "XTEE-CI-XM"),
        ("succeeded", True),
    ]

def run(client_security_server_address, producer_security_server_address,
        ssh_user, request_template_dir):

    xroad_request_template_filename = os.path.join(
            request_template_dir, "simple_xroad_query_template.xml")
    query_operational_data_producer_ss1_client_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_producer_ss1_client_template.xml")
    query_operational_data_client_ss0_owner_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_ss0_owner_template.xml")
    query_operational_data_client_ss_owner_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_ss_owner_template.xml")
    query_operational_data_client_central_monitoring_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_central_monitoring_template.xml")
    query_operational_data_client_ss_owner_filtered_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_ss_owner_filtered_template.xml")
    query_operational_data_client_central_monitoring_filtered_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_central_monitoring_filtered_template.xml")
    query_data_client_template_filename = os.path.join(
            request_template_dir, "query_operational_data_client_template.xml")
    query_data_client_filtered_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_filtered_template.xml")
    query_operational_data_client_ss0_owner_filtered_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_ss0_owner_filtered_template.xml")
    query_data_client_invalid_filter_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_invalid_filter_template.xml")
    query_data_client_unknown_member_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_unknown_member_template.xml")
    query_data_client_unknown_subsystem_template_filename = os.path.join(
            request_template_dir,
            "query_operational_data_client_unknown_subsystem_template.xml")

    client_timestamp_before_requests = common.get_remote_timestamp(
            client_security_server_address, ssh_user)
    producer_timestamp_before_requests = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)

    xroad_message_id = common.generate_message_id()
    print("\nGenerated message ID %s for X-Road request" % (xroad_message_id, ))

    ### Regular and operational data requests and the relevant checks
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

    # Send an operational data request from SS1 client to the producer's 
    # security server

    message_id = common.generate_message_id()
    message_id_producer = message_id
    print("\n---- Sending an operational data request from SS1 client" \
            " to the producer's security server ----\n")

    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_producer_ss1_client_template_filename,
            message_id, 1, 2)
    print("Generated the following operational data request for the producer's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(client_security_server_address, 
                                       request_contents, get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(
                mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

    else:
        common.parse_and_check_soap_response(raw_response)

    # Send an operational data request from SS0 owner to the client's 
    # security server

    message_id = common.generate_message_id()
    message_id_client = message_id
    print("\n---- Sending an operational data request from the SS0 owner" \
            " to the client's security server ----\n")

    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_ss0_owner_template_filename,
            message_id, 1, 2)
    print("Generated the following operational data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(producer_security_server_address, 
                                       request_contents, get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(
                mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

    else:
        common.parse_and_check_soap_response(raw_response)

    client_timestamp_after_requests = common.get_remote_timestamp(
            client_security_server_address, ssh_user)
    producer_timestamp_after_requests = common.get_remote_timestamp(
            producer_security_server_address, ssh_user)

    # Now make operational data requests to client's security server as 
    # security server owner, central monitoring client and regular client and 
    # check the response payloads.

    print("\n---- Sending an operational data request from the security server " \
            "owner to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_ss_owner_template_filename, message_id,
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

        # Security server owner is expected to receive all three query records.
        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the X-Road request at the client proxy side in the
        # client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_simple_query_rec(
                    xroad_message_id, client_security_server_address, "Client"))

        # The record describing the query data request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_query_data_client_proxy_rec(
                    message_id_producer, client_security_server_address, "Client"))

        # The record describing the query data request at the server proxy 
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_query_data_server_proxy_rec(
                    message_id_client, client_security_server_address, "Producer"))

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from the central " \
            "monitoring client to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_central_monitoring_template_filename,
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

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

        # Central monitoring client is expected to receive all three query 
        # records.
        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the X-Road request at the client proxy side 
        # in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_simple_query_rec(
                    xroad_message_id, client_security_server_address, "Client"))

        # The record describing the query data request at the client proxy 
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_query_data_client_proxy_rec(
                    message_id_producer, client_security_server_address, "Client"))

        # The record describing the query data request at the server proxy 
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_query_data_server_proxy_rec(
                    message_id_client, client_security_server_address, "Producer"))

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload, client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from the security server " \
            "owner with the member 'GOV:00000000' in search criteria " \
            "to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_ss_owner_filtered_template_filename, 
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

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

        # With the member 'GOV:00000000' in search criteria,
        # security server owner is expected to receive only the query
        # record where the member 'GOV:00000000' is the service provider.
        common.check_record_count(record_count, 1)

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the query data request at the client proxy side in the
        # client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_query_data_client_proxy_rec(
                    message_id_producer, client_security_server_address, "Client"))

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests, client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from the central monitoring " \
            "client with the subsystem 'GOV:00000000:Center' in search " \
            "criteria to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_central_monitoring_filtered_template_filename,
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

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

        # With the subsystem 'GOV:00000000:Center' in search criteria,
        # central monitoring client is expected to receive only the query
        # record where the subsystem 'GOV:00000000:Center' is
        # the service provider.
        common.check_record_count(record_count, 1)

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the X-Road request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, _expected_keys_and_values_of_simple_query_rec(
                    xroad_message_id, client_security_server_address, "Client"))

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests, client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from regular client " \
            "'GOV:00000001:System1' to the client's security server ----\n")

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

        # Regular client 'GOV:00000001:System1' is expected to receive the two
        # query records where the subsystem 'GOV:00000001:System1' is the client.
        common.check_record_count(record_count, 2)

        # As operational data is queried by regular client, the field
        # 'securityServerInternalIp' is not expected to be included 
        # in the response payload.
        common.assert_missing_in_json(json_payload, "securityServerInternalIp")

        # Remove the field 'securityServerInternalIp' from expected keys
        # and values lists
        expected_keys_and_valuesof_simple_query_rec = _expected_keys_and_values_of_simple_query_rec(
                xroad_message_id, client_security_server_address, "Client")
        expected_keys_and_values_of_query_data_client_proxy_rec = _expected_keys_and_values_of_query_data_client_proxy_rec(
                message_id_producer, client_security_server_address, "Client")
        list_of_expected_keys_and_values = [expected_keys_and_valuesof_simple_query_rec,
                                           expected_keys_and_values_of_query_data_client_proxy_rec]
        common.remove_key_from_list(
                "securityServerInternalIp", list_of_expected_keys_and_values)

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the X-Road request at the client proxy side in the
        # client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_valuesof_simple_query_rec)

        # The record describing the query data request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_values_of_query_data_client_proxy_rec)

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from regular client " \
            "'GOV:00000000' to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_ss0_owner_template_filename, message_id,
            client_timestamp_before_requests, client_timestamp_after_requests)

    print("Generated the following query data request for the client's security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            producer_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

        json_payload = common.get_multipart_json_payload(mime_parts[1])

        # Regular client 'GOV:00000000' is expected to receive two records: the 
        # query record where the member 'GOV:00000000' is the client and the
        # query record where the member 'GOV:00000000' is the service provider.
        common.check_record_count(record_count, 2)

        # As operational data is queried by regular client, the field
        # 'securityServerInternalIp' is not expected to be included 
        # in the response payload.
        common.assert_missing_in_json(json_payload, "securityServerInternalIp")

        # Remove the field 'securityServerInternalIp' from expected keys 
        # and values list
        expected_keys_and_values_of_query_data_server_proxy_rec = _expected_keys_and_values_of_query_data_server_proxy_rec(
                    message_id_client, client_security_server_address, "Producer")
        common.remove_key_from_list(
                "securityServerInternalIp",
                [expected_keys_and_values_of_query_data_server_proxy_rec])

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the query data request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_values_of_query_data_client_proxy_rec)

        # The record describing the query data request at the server proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_values_of_query_data_server_proxy_rec)

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from regular client " \
            "'GOV:00000001:System1' with the member 'GOV:00000000' in " \
            "search criteria to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_data_client_filtered_template_filename, message_id,
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

        # With the member 'GOV:00000000' in search criteria,
        # regular client 'GOV:00000001:System1' is expected to receive one
        # query record where the subsystem 'GOV:00000001:System1' is the client
        # and the member 'GOV:00000000' is the service provider.
        common.check_record_count(record_count, 1)

        # As operational data is queried by regular client, the field
        # 'securityServerInternalIp' is not expected to be included 
        # in the response payload.
        common.assert_missing_in_json(json_payload, "securityServerInternalIp")

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the query data request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_values_of_query_data_client_proxy_rec)

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request from regular client " \
            "'GOV:0000000' with the member 'GOV:00000000' in " \
            "search criteria to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_operational_data_client_ss0_owner_filtered_template_filename,
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

    print("Generated the following query data request for the client's security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            producer_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

        json_payload = common.get_multipart_json_payload(mime_parts[1])

        # With the member 'GOV:00000000' in search criteria,
        # regular client 'GOV:00000000' is expected to receive one
        # query record where the member 'GOV:00000000' is the service provider.
        common.check_record_count(record_count, 1)

        # As operational data is queried by regular client, the field
        # 'securityServerInternalIp' is not expected to be included 
        # in the response payload.
        common.assert_missing_in_json(json_payload, "securityServerInternalIp")

        # Check the presence of all the required fields in at least one JSON structure.

        # The record describing the query data request at the client proxy
        # side in the client's security server
        common.assert_present_in_json(
                json_payload, expected_keys_and_values_of_query_data_client_proxy_rec)

        # Check timestamp values.
        common.assert_expected_timestamp_values(
                json_payload,
                client_timestamp_before_requests,
                client_timestamp_after_requests)

        common.print_multipart_query_data_response(json_payload)

    else:
        common.parse_and_check_soap_response(raw_response)

    message_id = common.generate_message_id()
    print("\n---- Sending an operational data request with invalid client" \
            " in search criteria to the client's security server ----\n")

    request_contents = common.format_query_operational_data_request_template(
            query_data_client_invalid_filter_template_filename, 
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)
    print("Generated the following operational data request for the client's " \
            "security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents)

    print("\nReceived the following X-Road response: \n")
    xml = common.parse_and_clean_xml(response.text)
    print(xml.toprettyxml())

    # Invalid client in search criteria of the operational monitoring request 
    # must result in a SOAP fault
    common.assert_soap_fault(xml)

    print("\n---- Sending an operational data request with an unknown member " \
            "in search criteria to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_data_client_unknown_member_template_filename,
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

    print("Generated the following query data request for the client's security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

        # Unknown member in search criteria must result in an empty response
        common.check_record_count(record_count, 0)

    else:
        common.parse_and_check_soap_response(raw_response)

    print("\n---- Sending an operational data request with an unknown " \
            "subsystem in search criteria to the client's security server ----\n")

    message_id = common.generate_message_id()
    print("Generated message ID %s for query data request" % (message_id, ))
 
    request_contents = common.format_query_operational_data_request_template(
            query_data_client_unknown_subsystem_template_filename,
            message_id, client_timestamp_before_requests,
            client_timestamp_after_requests)

    print("Generated the following query data request for the client's security server: \n")
    print(request_contents)

    response = common.post_xml_request(
            client_security_server_address, request_contents,
            get_raw_stream=True)
    mime_parts, raw_response = common.parse_multipart_response(response)

    if mime_parts:
        soap_part, record_count = common.get_multipart_soap_and_record_count(mime_parts[0])
        common.print_multipart_soap_and_record_count(soap_part, record_count)

        # Unknown subsystem in search criteria must result in an empty response
        common.check_record_count(record_count, 0)

    else:
        common.parse_and_check_soap_response(raw_response)
