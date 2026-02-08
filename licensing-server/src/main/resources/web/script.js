const API_URL = 'http://localhost:8080/api';
let allLicenses = [];

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
        case 'dashboard':
            loadDashboard();
            break;
        case 'products':
            loadProducts();
            break;
        case 'licenses':
            loadLicenses();
            break;
        case 'tiers':
            loadTiers();
            break;
        case 'logs':
            loadLogs();
            break;
    }
}

// Dashboard
async function loadDashboard() {
    try {
        const response = await fetch(`${API_URL}/stats`);
        const stats = await response.json();
        
        document.getElementById('stat-products').textContent = stats.totalProducts || 0;
        document.getElementById('stat-licenses').textContent = stats.totalLicenses || 0;
        document.getElementById('stat-active').textContent = stats.activeLicenses || 0;
        document.getElementById('stat-expired').textContent = stats.expiredLicenses || 0;
        document.getElementById('stat-tiers').textContent = stats.totalTiers || 0;
        document.getElementById('stat-validations').textContent = stats.validationsToday || 0;
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

// Products
async function loadProducts() {
    const container = document.getElementById('products-list');
    container.innerHTML = '<div class="loading">Loading products</div>';
    
    try {
        const response = await fetch(`${API_URL}/products`);
        const products = await response.json();
        
        if (products.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>No products yet. Create your first product!</p></div>';
            return;
        }
        
        container.innerHTML = products.map(product => `
            <div class="card">
                <div class="card-header">
                    <h3>${escapeHtml(product.name)}</h3>
                    <div class="card-actions">
                        <button class="btn-icon danger" onclick="confirmDelete('product', '${escapeHtml(product.id)}', '${escapeHtml(product.name)}')" title="Delete product">Delete</button>
                    </div>
                </div>
                <p>${escapeHtml(product.description || 'No description')}</p>
                <div class="card-meta">
                    <span>ID: ${escapeHtml(product.id).substring(0, 8)}...</span>
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
        
        await response.json();
        
        closeModal('add-product-modal');
        showSuccess('Product created successfully!');
        loadProducts();
        
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating product:', error);
        showError('Failed to create product');
    }
});

// Licenses
async function loadLicenses() {
    const container = document.getElementById('licenses-list');
    container.innerHTML = '<div class="loading">Loading licenses</div>';
    
    try {
        const response = await fetch(`${API_URL}/licenses`);
        allLicenses = await response.json();
        
        renderLicenses(allLicenses);
        
    } catch (error) {
        console.error('Error loading licenses:', error);
        showError('Failed to load licenses');
    }
}

function filterLicenses() {
    const query = document.getElementById('license-search').value.toLowerCase();
    if (!query) {
        renderLicenses(allLicenses);
        return;
    }
    
    const filtered = allLicenses.filter(license =>
        (license.key && license.key.toLowerCase().includes(query)) ||
        (license.product_name && license.product_name.toLowerCase().includes(query)) ||
        (license.tier_name && license.tier_name.toLowerCase().includes(query)) ||
        (license.hwid && license.hwid.toLowerCase().includes(query)) ||
        (license.ip && license.ip.toLowerCase().includes(query))
    );
    
    renderLicenses(filtered);
}

function renderLicenses(licenses) {
    const container = document.getElementById('licenses-list');
    
    if (licenses.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>No licenses found.</p></div>';
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
                    <th>Expiry</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${licenses.map(license => `
                    <tr>
                        <td>
                            <code>${escapeHtml(license.key)}</code>
                            <button class="copy-btn" onclick="copyToClipboard('${escapeHtml(license.key)}')">Copy</button>
                        </td>
                        <td>${escapeHtml(license.product_name)}</td>
                        <td>${escapeHtml(license.tier_name || 'Full Access')}</td>
                        <td>${license.hwid ? escapeHtml(license.hwid).substring(0, 12) + '...' : '<span style="color:var(--text-secondary)">Not bound</span>'}</td>
                        <td>${license.expiry_time ? formatDate(license.expiry_time) : '<span style="color:var(--text-secondary)">Never</span>'}</td>
                        <td>${getLicenseStatusBadge(license)}</td>
                        <td>
                            <div class="action-cell">
                                <button class="btn-icon" onclick="toggleLicense('${escapeHtml(license.key)}', ${!license.active})" title="${license.active ? 'Deactivate' : 'Activate'}">
                                    ${license.active ? 'Pause' : 'Start'}
                                </button>
                                ${license.hwid ? `<button class="btn-icon" onclick="resetHwid('${escapeHtml(license.key)}')" title="Reset HWID">Reset</button>` : ''}
                                <button class="btn-icon danger" onclick="confirmDelete('license', '${escapeHtml(license.key)}', '${escapeHtml(license.key)}')" title="Delete">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function getLicenseStatusBadge(license) {
    if (!license.active) {
        return '<span class="badge badge-danger">Inactive</span>';
    }
    
    if (license.expiry_time && license.expiry_time > 0 && Date.now() > license.expiry_time) {
        return '<span class="badge badge-warning">Expired</span>';
    }
    
    return '<span class="badge badge-success">Active</span>';
}

async function toggleLicense(key, active) {
    try {
        await fetch(`${API_URL}/licenses/${key}/toggle`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ active })
        });
        
        showSuccess(active ? 'License activated' : 'License deactivated');
        loadLicenses();
    } catch (error) {
        console.error('Error toggling license:', error);
        showError('Failed to update license');
    }
}

async function resetHwid(key) {
    showConfirm('Reset HWID Binding', 'This will unbind the license from its current hardware. The next validation will bind it to new hardware.', async () => {
        try {
            await fetch(`${API_URL}/licenses/${key}/reset-hwid`, {
                method: 'PUT'
            });
            
            showSuccess('HWID binding reset successfully');
            loadLicenses();
        } catch (error) {
            console.error('Error resetting HWID:', error);
            showError('Failed to reset HWID');
        }
    });
}

async function showAddLicense() {
    // Ensure products are loaded for dropdown
    try {
        const response = await fetch(`${API_URL}/products`);
        const products = await response.json();
        updateProductDropdowns(products);
    } catch (e) { /* dropdowns may already be populated */ }
    
    document.getElementById('add-license-modal').classList.add('active');
}

async function loadTiersForProduct(productId, selectId) {
    const tierSelect = document.getElementById(selectId);
    tierSelect.innerHTML = '<option value="">No Tier (Full Access)</option>';
    
    if (!productId) return;
    
    try {
        const response = await fetch(`${API_URL}/products/${productId}/tiers`);
        const tiers = await response.json();
        
        tiers.forEach(tier => {
            const option = document.createElement('option');
            option.value = tier.id;
            option.textContent = tier.name;
            tierSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading tiers for product:', error);
    }
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
        
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating license:', error);
        showError('Failed to create license');
    }
});

// Tiers
async function loadTiers() {
    const container = document.getElementById('tiers-list');
    container.innerHTML = '<div class="loading">Loading tiers</div>';
    
    try {
        const productsResponse = await fetch(`${API_URL}/products`);
        const products = await productsResponse.json();
        
        let html = '';
        
        for (const product of products) {
            const tiersResponse = await fetch(`${API_URL}/products/${product.id}/tiers`);
            const tiers = await tiersResponse.json();
            
            if (tiers.length > 0) {
                tiers.forEach(tier => {
                    html += `
                        <div class="card">
                            <div class="card-header">
                                <h3>${escapeHtml(tier.name)}</h3>
                                <div class="card-actions">
                                    <button class="btn-icon danger" onclick="confirmDelete('tier', '${escapeHtml(tier.id)}', '${escapeHtml(tier.name)}')" title="Delete tier">Delete</button>
                                </div>
                            </div>
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
        
        container.innerHTML = html || '<div class="empty-state"><p>No tiers yet. Create pricing tiers for your products!</p></div>';
        
    } catch (error) {
        console.error('Error loading tiers:', error);
        showError('Failed to load tiers');
    }
}

async function showAddTier() {
    // Ensure products are loaded for dropdown
    try {
        const response = await fetch(`${API_URL}/products`);
        const products = await response.json();
        updateProductDropdowns(products);
    } catch (e) { /* dropdowns may already be populated */ }
    
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
        
        await response.json();
        
        closeModal('add-tier-modal');
        showSuccess('Tier created successfully!');
        loadTiers();
        
        e.target.reset();
        
    } catch (error) {
        console.error('Error creating tier:', error);
        showError('Failed to create tier');
    }
});

// Validation Logs
async function loadLogs() {
    const container = document.getElementById('logs-list');
    container.innerHTML = '<div class="loading">Loading logs</div>';
    
    try {
        const response = await fetch(`${API_URL}/logs?limit=100`);
        const logs = await response.json();
        
        if (logs.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>No validation logs yet.</p></div>';
            return;
        }
        
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Time</th>
                        <th>License Key</th>
                        <th>Product</th>
                        <th>HWID</th>
                        <th>IP</th>
                        <th>Result</th>
                    </tr>
                </thead>
                <tbody>
                    ${logs.map(log => `
                        <tr>
                            <td>${formatDateTime(log.timestamp)}</td>
                            <td><code>${escapeHtml(log.license_key)}</code></td>
                            <td>${escapeHtml(log.product_name || 'N/A')}</td>
                            <td>${log.hwid ? escapeHtml(log.hwid).substring(0, 12) + '...' : 'N/A'}</td>
                            <td>${escapeHtml(log.ip || 'N/A')}</td>
                            <td>${log.success ? '<span class="badge badge-success">Success</span>' : '<span class="badge badge-danger">Failed</span>'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
    } catch (error) {
        console.error('Error loading logs:', error);
        showError('Failed to load validation logs');
    }
}

// Delete operations
function confirmDelete(type, id, name) {
    const messages = {
        product: `Are you sure you want to delete the product "${name}"? This action cannot be undone.`,
        license: `Are you sure you want to delete license key "${name}"? This action cannot be undone.`,
        tier: `Are you sure you want to delete the tier "${name}"? This action cannot be undone.`
    };
    
    showConfirm('Delete ' + type.charAt(0).toUpperCase() + type.slice(1), messages[type], async () => {
        try {
            const urls = {
                product: `${API_URL}/products/${id}`,
                license: `${API_URL}/licenses/${id}`,
                tier: `${API_URL}/tiers/${id}`
            };
            
            await fetch(urls[type], { method: 'DELETE' });
            
            showSuccess(`${type.charAt(0).toUpperCase() + type.slice(1)} deleted successfully`);
            
            // Reload the appropriate tab
            const reloadFns = { product: loadProducts, license: loadLicenses, tier: loadTiers };
            reloadFns[type]();
            
        } catch (error) {
            console.error(`Error deleting ${type}:`, error);
            showError(`Failed to delete ${type}`);
        }
    });
}

// Confirm dialog
function showConfirm(title, message, onConfirm) {
    document.getElementById('confirm-title').textContent = title;
    document.getElementById('confirm-message').textContent = message;
    
    const confirmBtn = document.getElementById('confirm-action-btn');
    const newBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newBtn, confirmBtn);
    newBtn.id = 'confirm-action-btn';
    
    newBtn.addEventListener('click', () => {
        closeModal('confirm-modal');
        onConfirm();
    });
    
    document.getElementById('confirm-modal').classList.add('active');
}

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
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDate(timestamp) {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatDateTime(timestamp) {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// Toast notifications
function showSuccess(message) {
    showToast(message, 'toast-success');
}

function showError(message) {
    showToast(message, 'toast-error');
}

function showToast(message, type) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('toast-out');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Close modals when clicking outside
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('active');
        }
    });
});

// Close modals with Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal.active').forEach(modal => {
            modal.classList.remove('active');
        });
    }
});

// Initialize
loadDashboard();
