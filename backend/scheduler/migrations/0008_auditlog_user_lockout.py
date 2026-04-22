from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('scheduler', '0007_schedule_soft_delete'),
    ]

    operations = [
        migrations.AddField(
            model_name='user',
            name='failed_login_attempts',
            field=models.PositiveSmallIntegerField(default=0),
        ),
        migrations.AddField(
            model_name='user',
            name='locked_until',
            field=models.DateTimeField(blank=True, null=True, verbose_name='Kilitli Kalma Süresi'),
        ),
        migrations.CreateModel(
            name='AuditLog',
            fields=[
                ('id', models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('action', models.CharField(choices=[('CREATE', 'Oluşturma'), ('UPDATE', 'Güncelleme'), ('DELETE', 'Silme'), ('LOGIN', 'Giriş'), ('LOGIN_FAIL', 'Başarısız Giriş')], max_length=20)),
                ('model_name', models.CharField(blank=True, max_length=100)),
                ('object_id', models.CharField(blank=True, max_length=50)),
                ('description', models.TextField(blank=True)),
                ('ip_address', models.GenericIPAddressField(blank=True, null=True)),
                ('created_at', models.DateTimeField(auto_now_add=True)),
                ('actor', models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, related_name='audit_logs', to='scheduler.user')),
            ],
            options={
                'verbose_name': 'Denetim Kaydı',
                'verbose_name_plural': 'Denetim Kayıtları',
                'ordering': ['-created_at'],
            },
        ),
    ]
