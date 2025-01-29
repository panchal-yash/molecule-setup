import testinfra

def test_service_exists(host):
    service = host.service("mysql")
    assert service.exists

def test_service_is_running(host):
    service = host.service("mysql")
    assert service.is_running

