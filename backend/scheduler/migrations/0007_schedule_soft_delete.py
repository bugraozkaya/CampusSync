from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('scheduler', '0006_remove_user_plain_password'),
    ]

    operations = [
        migrations.AddField(
            model_name='schedule',
            name='is_deleted',
            field=models.BooleanField(default=False, verbose_name='Silindi'),
        ),
        migrations.AddField(
            model_name='schedule',
            name='deleted_at',
            field=models.DateTimeField(blank=True, null=True, verbose_name='Silinme Tarihi'),
        ),
    ]
