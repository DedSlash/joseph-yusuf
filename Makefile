ENV ?= staging
BUILD ?= latest
ANSIBLE_DIR = ansible
VAULT_PASS = $(ANSIBLE_DIR)/vault/.vault_pass

.PHONY: setup deploy rollback logs status

setup:
	cd $(ANSIBLE_DIR) && ansible-playbook playbooks/setup.yml \
		-i inventory/$(ENV).yml \
		--vault-password-file $(VAULT_PASS)

deploy:
	cd $(ANSIBLE_DIR) && ansible-playbook playbooks/deploy.yml \
		-i inventory/$(ENV).yml \
		-e build_number=$(BUILD) \
		--vault-password-file $(VAULT_PASS)

rollback:
	cd $(ANSIBLE_DIR) && ansible-playbook playbooks/rollback.yml \
		-i inventory/$(ENV).yml \
		-e rollback_build=$(BUILD) \
		--vault-password-file $(VAULT_PASS)

logs:
	ssh deploy@$$(grep ansible_host $(ANSIBLE_DIR)/inventory/$(ENV).yml | head -1 | awk '{print $$2}') \
		"cd /opt/joseph-yusuf && docker compose logs -f --tail=100"

status:
	ssh deploy@$$(grep ansible_host $(ANSIBLE_DIR)/inventory/$(ENV).yml | head -1 | awk '{print $$2}') \
		"cd /opt/joseph-yusuf && docker compose ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}'"
