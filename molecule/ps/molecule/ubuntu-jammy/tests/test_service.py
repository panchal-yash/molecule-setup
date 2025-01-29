import testinfra

def test_service_exists(host):
    service = host.service("mysql")
    assert service.exists

def test_service_is_running(host):
    service = host.service("mysql")
    assert service.is_running

def test_service_is_notrunning(host):
    service = host.service("mysql")
    assert not service.is_running
