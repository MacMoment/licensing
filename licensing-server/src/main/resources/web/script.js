const API_URL = 'http://localhost:8080/api';

// Tab switching
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        
        // Update active states
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        
        btn.classList.add('active');
        document.getElementById(tab).classList.add('active');
        
        // Load data for active tab
        loadTabData(tab);
    });
});

// Load data based on active tab
function loadTabData(tab) {
    switch(tab) {
        case 'products':
            loadProducts();
            break;
        case 'licenses':
            loadLicenses();
            break;
        case 'tiers':
            loadTiers();
            break;
    }
}

// Products
async function loadProducts() {
    try {
        const response = await fetch(`${API_URL}/products`);
        const products = await response.json();
        
        const container = document.getElementById('products-list');
        
        if (products.length === 0) {
            container.innerHTML = '<div class="empty-state">No products yet. Create your first product!</div>';
            return;
        }
        
        container.innerHTML = products.map(product => `
            <div class="card">
                <h3>${escapeHtml(product.name)}</h3>
                <p>${escapeHtml(product.description || 'No description')}</p>
                <div class="card-meta">
                    <span>ID: ${escapeHtml(product.id)}</span>
                    <span>${formatDate(product.created_at)}</span>
                </div>
            </div>
        `).join('');
        
        // Update product dropdowns
        updateProductDropdowns(products);
        
    } catch (error) {
        console.error('Error loading products:', error);
        showError('Failed to load products');
    }
}

async function showAddProduct() {
    document.getElementById('add-product-modal').classList.add('active');
}

document.getElementById('add-product-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const data = {
        name: document.getElementById('product-name').value,
        description: document.getElementById('product-description').value
    };
    
    try {
        const response = await fetch(`${API_URL}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        closeModal('add-product-modal');
        showSuccess('Product created successfully!');
        loadProducts();
        
        // Reset form
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating product:', error);
        showError('Failed to create product');
    }
});

// Licenses
async function loadLicenses() {
    try {
        const response = await fetch(`${API_URL}/licenses`);
        const licenses = await response.json();
        
        const container = document.getElementById('licenses-list');
        
        if (licenses.length === 0) {
            container.innerHTML = '<div class="empty-state">No licenses yet. Generate your first license key!</div>';
            return;
        }
        
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>License Key</th>
                        <th>Product</th>
                        <th>Tier</th>
                        <th>HWID</th>
                        <th>IP</th>
                        <th>Expiry</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${licenses.map(license => `
                        <tr>
                            <td>
                                <code>${escapeHtml(license.key)}</code>
                                <button class="copy-btn" onclick="copyToClipboard('${escapeHtml(license.key)}')">📋</button>
                            </td>
                            <td>${escapeHtml(license.product_name)}</td>
                            <td>${escapeHtml(license.tier_name || 'Full Access')}</td>
                            <td>${escapeHtml(license.hwid || 'Not bound')}</td>
                            <td>${escapeHtml(license.ip || 'N/A')}</td>
                            <td>${license.expiry_time ? formatDate(license.expiry_time) : 'Never'}</td>
                            <td>${getLicenseStatusBadge(license)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
    } catch (error) {
        console.error('Error loading licenses:', error);
        showError('Failed to load licenses');
    }
}

function getLicenseStatusBadge(license) {
    if (!license.active) {
        return '<span class="badge badge-danger">Inactive</span>';
    }
    
    if (license.expiry_time && license.expiry_time > 0 && Date.now() > license.expiry_time) {
        return '<span class="badge badge-danger">Expired</span>';
    }
    
    return '<span class="badge badge-success">Active</span>';
}

async function showAddLicense() {
    document.getElementById('add-license-modal').classList.add('active');
}

document.getElementById('add-license-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const expiryDate = document.getElementById('license-expiry').value;
    const expiryTime = expiryDate ? new Date(expiryDate).getTime() : null;
    
    const data = {
        productId: document.getElementById('license-product').value,
        tierId: document.getElementById('license-tier').value || null,
        expiryTime: expiryTime
    };
    
    try {
        const response = await fetch(`${API_URL}/licenses`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        closeModal('add-license-modal');
        showSuccess(`License created: ${result.key}`);
        loadLicenses();
        
        // Reset form
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating license:', error);
        showError('Failed to create license');
    }
});

// Tiers
async function loadTiers() {
    try {
        // Load all products first
        const productsResponse = await fetch(`${API_URL}/products`);
        const products = await response.json();
        
        const container = document.getElementById('tiers-list');
        container.innerHTML = '';
        
        for (const product of products) {
            const tiersResponse = await fetch(`${API_URL}/products/${product.id}/tiers`);
            const tiers = await tiersResponse.json();
            
            if (tiers.length > 0) {
                tiers.forEach(tier => {
                    container.innerHTML += `
                        <div class="card">
                            <h3>${escapeHtml(tier.name)}</h3>
                            <p>Product: ${escapeHtml(product.name)}</p>
                            <div class="card-meta">
                                <span>Features: ${escapeHtml(tier.features || 'None')}</span>
                                <span>Max Users: ${tier.max_users || 'Unlimited'}</span>
                            </div>
                        </div>
                    `;
                });
            }
        }
        
        if (container.innerHTML === '') {
            container.innerHTML = '<div class="empty-state">No tiers yet. Create pricing tiers for your products!</div>';
        }
        
    } catch (error) {
        console.error('Error loading tiers:', error);
        showError('Failed to load tiers');
    }
}

async function showAddTier() {
    document.getElementById('add-tier-modal').classList.add('active');
}

document.getElementById('add-tier-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const productId = document.getElementById('tier-product').value;
    const data = {
        name: document.getElementById('tier-name').value,
        features: document.getElementById('tier-features').value,
        maxUsers: parseInt(document.getElementById('tier-max-users').value) || 0
    };
    
    try {
        const response = await fetch(`${API_URL}/products/${productId}/tiers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        closeModal('add-tier-modal');
        showSuccess('Tier created successfully!');
        loadTiers();
        
        // Reset form
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating tier:', error);
        showError('Failed to create tier');
    }
});

// Helper functions
function updateProductDropdowns(products) {
    const licenseSelect = document.getElementById('license-product');
    const tierSelect = document.getElementById('tier-product');
    
    const options = products.map(p => 
        `<option value="${escapeHtml(p.id)}">${escapeHtml(p.name)}</option>`
    ).join('');
    
    licenseSelect.innerHTML = '<option value="">Select Product</option>' + options;
    tierSelect.innerHTML = '<option value="">Select Product</option>' + options;
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showSuccess('Copied to clipboard!');
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(timestamp) {
    return new Date(timestamp).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function showSuccess(message) {
    // Simple alert for now - could be enhanced with toast notifications
    alert('✓ ' + message);
}

function showError(message) {
    alert('✗ ' + message);
}

// Close modals when clicking outside
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('active');
        }
    });
});

// Initialize
loadProducts();
