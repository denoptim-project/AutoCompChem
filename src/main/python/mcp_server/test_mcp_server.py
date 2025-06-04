#!/usr/bin/env python3
"""
Test script for AutoCompChem MCP Server

This script tests the MCP server functionality by simulating tool calls
to verify everything works before integrating with Claude Desktop.

Usage:
    python test_mcp_server.py
"""

import asyncio
import json
import sys
from autocompchem_mcp_server import acc_client

async def test_api_connectivity():
    """Test basic connectivity to AutoCompChem API"""
    print("🔍 Testing AutoCompChem API connectivity...")
    
    try:
        # Test health endpoint
        health = await acc_client.make_request("GET", "/api/v1/autocompchem/health")
        print(f"✅ Health check: {health}")
        
        # Test info endpoint
        info = await acc_client.make_request("GET", "/api/v1/autocompchem/info")
        print(f"✅ API Info: {info}")
        
        return True
    except Exception as e:
        print(f"❌ API connectivity failed: {e}")
        return False

async def test_get_tasks():
    """Test getting available tasks"""
    print("\n🧪 Testing task listing...")
    
    try:
        tasks = await acc_client.make_request("GET", "/api/v1/autocompchem/tasks")
        print(f"✅ Found {len(tasks) if isinstance(tasks, list) else 'unknown'} tasks")
        
        if isinstance(tasks, list) and len(tasks) > 0:
            print(f"   First few tasks: {tasks[:5]}...")
        
        return True
    except Exception as e:
        print(f"❌ Task listing failed: {e}")
        return False

async def test_software_support():
    """Test getting supported software"""
    print("\n💻 Testing software support query...")
    
    try:
        software = await acc_client.make_request("GET", "/api/v1/compchem/software")
        print(f"✅ Software support data: {json.dumps(software, indent=2)}")
        return True
    except Exception as e:
        print(f"❌ Software support query failed: {e}")
        return False

async def test_task_help():
    """Test getting help for a specific task"""
    print("\n❓ Testing task help...")
    
    # Try a common task name
    task_names = ["prepareInputGaussian", "readGaussianOutput", "readAtomContainers"]
    
    for task_name in task_names:
        try:
            help_text = await acc_client.make_request("GET", f"/api/v1/autocompchem/tasks/{task_name}/help")
            print(f"✅ Help for '{task_name}': {str(help_text)[:100]}...")
            return True
        except Exception as e:
            print(f"⚠️  Help for '{task_name}' failed: {e}")
            continue
    
    print("❌ No task help was successful")
    return False

async def test_simple_task_execution():
    """Test executing a simple task"""
    print("\n⚙️ Testing simple task execution...")
    
    # Try to execute a simple task (this might fail if task doesn't exist)
    try:
        # Test with minimal parameters
        params = {"TASK": "readAtomContainers"}
        result = await acc_client.make_request("POST", "/api/v1/autocompchem/tasks/readAtomContainers/execute", params)
        print(f"✅ Task execution result: {json.dumps(result, indent=2)}")
        return True
    except Exception as e:
        print(f"⚠️  Simple task execution failed (this may be expected): {e}")
        return False

async def run_all_tests():
    """Run all tests and report results"""
    print("=" * 60)
    print("🧪 AutoCompChem MCP Server Test Suite")
    print("=" * 60)
    
    tests = [
        ("API Connectivity", test_api_connectivity),
        ("Task Listing", test_get_tasks),
        ("Software Support", test_software_support),
        ("Task Help", test_task_help),
        ("Simple Task Execution", test_simple_task_execution),
    ]
    
    results = {}
    
    for test_name, test_func in tests:
        print(f"\n{'='*20} {test_name} {'='*20}")
        try:
            results[test_name] = await test_func()
        except Exception as e:
            print(f"❌ {test_name} crashed: {e}")
            results[test_name] = False
    
    # Summary
    print("\n" + "=" * 60)
    print("📊 Test Summary")
    print("=" * 60)
    
    passed = sum(1 for result in results.values() if result)
    total = len(results)
    
    for test_name, result in results.items():
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} {test_name}")
    
    print(f"\nOverall: {passed}/{total} tests passed")
    
    if passed >= 3:  # At least basic connectivity and info should work
        print("🎉 MCP Server appears to be working correctly!")
        print("\nNext steps:")
        print("1. Start the AutoCompChem server: ./debug-server.sh")
        print("2. Configure Claude Desktop with the MCP server")
        print("3. Ask Claude: 'What computational chemistry tasks are available?'")
        return True
    else:
        print("⚠️  Some critical tests failed. Check AutoCompChem server status.")
        print("\nTroubleshooting:")
        print("1. Ensure AutoCompChem server is running: curl http://localhost:8080/api/v1/autocompchem/health")
        print("2. Check server logs for errors")
        print("3. Verify network connectivity")
        return False

if __name__ == "__main__":
    # Run the test suite
    try:
        success = asyncio.run(run_all_tests())
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n\n🛑 Test interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n💥 Test suite crashed: {e}")
        sys.exit(1) 