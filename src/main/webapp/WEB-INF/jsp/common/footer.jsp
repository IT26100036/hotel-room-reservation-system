<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
        </main>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // Live clock in the top bar
    (function () {
        const t = document.getElementById('clockTime');
        const d = document.getElementById('clockDate');
        function tick() {
            const now = new Date();
            t.textContent = now.toLocaleTimeString('en-GB');
            d.textContent = now.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
        }
        tick();
        setInterval(tick, 1000);
    })();
</script>
</body>
</html>
